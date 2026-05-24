using FinanceService.Config;
using FinanceService.Domain.Entities;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Exceptions;
using FinanceService.Services.Implementations;
using FinanceService.Services.Interfaces;
using FinanceService.Tests.Fakes;
using FluentAssertions;
using Microsoft.Extensions.Options;

namespace FinanceService.Tests.Services;

public sealed class PayPalPaymentServiceTests
{
    private readonly InMemoryPaymentRepository _repository = new();
    private readonly FakePayPalClient _payPalClient = new();
    private readonly FakePaymentEventPublisher _publisher = new();

    [Fact]
    public async Task CreateOrderAsync_withPendingPayment_shouldCallPayPalClient()
    {
        var payment = SeedPayment(PaymentStatus.Pending);
        var service = CreateService();

        await service.CreateOrderAsync(payment.Id);

        _payPalClient.CreateOrderCalls.Should().Be(1);
        _payPalClient.LastAmount.Should().Be(payment.Amount);
        _payPalClient.LastCurrency.Should().Be(payment.Currency);
        _payPalClient.LastReferenceId.Should().Be(payment.OrderId);
    }

    [Fact]
    public async Task CreateOrderAsync_shouldSaveProviderReference()
    {
        var payment = SeedPayment(PaymentStatus.Pending);
        var service = CreateService();

        var response = await service.CreateOrderAsync(payment.Id);
        var updatedPayment = await _repository.FindByIdAsync(payment.Id);

        response.PaypalOrderId.Should().Be(_payPalClient.PaypalOrderId);
        updatedPayment!.Provider.Should().Be("PAYPAL");
        updatedPayment.ProviderReference.Should().Be(_payPalClient.PaypalOrderId);
        updatedPayment.Status.Should().Be(PaymentStatus.Pending);
    }

    [Fact]
    public async Task CreateOrderAsync_whenPaymentMissing_shouldThrowNotFound()
    {
        var service = CreateService();

        var act = () => service.CreateOrderAsync(Guid.NewGuid());

        await act.Should().ThrowAsync<PaymentNotFoundException>();
    }

    [Fact]
    public async Task CreateOrderAsync_whenPaymentApproved_shouldThrowConflict()
    {
        var payment = SeedPayment(PaymentStatus.Approved);
        var service = CreateService();

        var act = () => service.CreateOrderAsync(payment.Id);

        await act.Should().ThrowAsync<InvalidPaymentStatusTransitionException>();
        _payPalClient.CreateOrderCalls.Should().Be(0);
    }

    [Fact]
    public async Task CaptureOrderAsync_whenCompleted_shouldApprovePayment()
    {
        var payment = SeedPayment(PaymentStatus.Pending, providerReference: "PAYPAL-ORDER-1");
        var service = CreateService();

        var response = await service.CaptureOrderAsync(
            payment.Id,
            new CapturePayPalOrderRequest { PaypalOrderId = "PAYPAL-ORDER-1" });
        var updatedPayment = await _repository.FindByIdAsync(payment.Id);

        response.PaymentStatus.Should().Be(PaymentStatus.Approved);
        updatedPayment!.Status.Should().Be(PaymentStatus.Approved);
        updatedPayment.Provider.Should().Be("PAYPAL");
        updatedPayment.ProviderReference.Should().Be("CAPTURE-1");
        updatedPayment.ApprovedAt.Should().NotBeNull();
    }

    [Fact]
    public async Task CaptureOrderAsync_whenCompleted_shouldPublishPaymentApproved()
    {
        var payment = SeedPayment(PaymentStatus.Pending, providerReference: "PAYPAL-ORDER-1");
        var service = CreateService();

        await service.CaptureOrderAsync(payment.Id, new CapturePayPalOrderRequest());

        _publisher.PaymentApprovedCalls.Should().Be(1);
        _publisher.LastApprovedPayment!.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task CaptureOrderAsync_whenPayPalFails_shouldNotPublishOrApprove()
    {
        var payment = SeedPayment(PaymentStatus.Pending, providerReference: "PAYPAL-ORDER-1");
        _payPalClient.CaptureOrderException = new PayPalPaymentException("PayPal failed.");
        var service = CreateService();

        var act = () => service.CaptureOrderAsync(payment.Id, new CapturePayPalOrderRequest());

        await act.Should().ThrowAsync<PayPalPaymentException>();
        _publisher.PaymentApprovedCalls.Should().Be(0);
        (await _repository.FindByIdAsync(payment.Id))!.Status.Should().Be(PaymentStatus.Pending);
    }

    [Theory]
    [InlineData(PaymentStatus.Cancelled)]
    [InlineData(PaymentStatus.Rejected)]
    [InlineData(PaymentStatus.Approved)]
    public async Task CaptureOrderAsync_whenPaymentNotPending_shouldThrowConflict(PaymentStatus status)
    {
        var payment = SeedPayment(status, providerReference: "PAYPAL-ORDER-1");
        var service = CreateService();

        var act = () => service.CaptureOrderAsync(payment.Id, new CapturePayPalOrderRequest());

        await act.Should().ThrowAsync<InvalidPaymentStatusTransitionException>();
        _payPalClient.CaptureOrderCalls.Should().Be(0);
        _publisher.PaymentApprovedCalls.Should().Be(0);
    }

    [Fact]
    public async Task CaptureOrderAsync_whenPayPalStatusIsNotCompleted_shouldKeepPaymentPending()
    {
        var payment = SeedPayment(PaymentStatus.Pending, providerReference: "PAYPAL-ORDER-1");
        _payPalClient.CaptureResult = new PayPalCaptureResult(
            "PAYPAL-ORDER-1",
            "DECLINED",
            "CAPTURE-1",
            "CAPTURE-1");
        var service = CreateService();

        var act = () => service.CaptureOrderAsync(payment.Id, new CapturePayPalOrderRequest());

        await act.Should().ThrowAsync<PayPalPaymentException>();
        _publisher.PaymentApprovedCalls.Should().Be(0);
        (await _repository.FindByIdAsync(payment.Id))!.Status.Should().Be(PaymentStatus.Pending);
    }

    private PayPalPaymentService CreateService()
    {
        return new PayPalPaymentService(
            _repository,
            _payPalClient,
            _publisher,
            Options.Create(new PayPalSettings { Currency = "MXN" }));
    }

    private Payment SeedPayment(
        PaymentStatus status,
        string? providerReference = null)
    {
        var payment = new Payment
        {
            Id = Guid.NewGuid(),
            OrderId = $"order-{Guid.NewGuid():N}",
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            Amount = 150.75m,
            Currency = "MXN",
            Status = status,
            ProviderReference = providerReference,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _repository.Seed(payment);

        return payment;
    }
}
