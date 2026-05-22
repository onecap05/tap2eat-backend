using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Exceptions;
using FinanceService.Services.Implementations;
using FinanceService.Tests.Fakes;
using FinanceService.Tests.TestData;
using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;

namespace FinanceService.Tests.Services;

public sealed class PaymentServiceImplTests
{
    private readonly InMemoryPaymentRepository _repository = new();
    private readonly FakePaymentEventPublisher _publisher = new();
    private readonly PaymentServiceImpl _service;

    public PaymentServiceImplTests()
    {
        _service = new PaymentServiceImpl(
            _repository,
            _publisher,
            NullLogger<PaymentServiceImpl>.Instance);
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_createsPendingPayment()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();

        var payment = await _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        payment.OrderId.Should().Be(orderEvent.OrderId);
        payment.Amount.Should().Be(orderEvent.Total);
        payment.Status.Should().Be(PaymentStatus.Pending);
        payment.Currency.Should().Be("MXN");
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_isIdempotentForSameOrderId()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();

        var firstPayment = await _service.CreatePendingPaymentFromOrderAsync(orderEvent);
        var secondPayment = await _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        secondPayment.Id.Should().Be(firstPayment.Id);
        secondPayment.OrderId.Should().Be(firstPayment.OrderId);
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_WhenPaymentAlreadyExists_ShouldReturnExistingAndNotDuplicate()
    {
        var existingPayment = PaymentTestData.Payment(orderId: "existing-order");
        _repository.Seed(existingPayment);

        var response = await _service.CreatePendingPaymentFromOrderAsync(
            PaymentTestData.OrderCreatedEvent("existing-order"));

        response.Id.Should().Be(existingPayment.Id);
        _publisher.PaymentApprovedCalls.Should().Be(0);
        _publisher.PaymentRejectedCalls.Should().Be(0);
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_rejectsTotalLessThanOrEqualToZero()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent(total: 0m);

        var action = () => _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        await action.Should().ThrowAsync<FinanceValidationException>();
    }

    [Fact]
    public async Task GetByIdAsync_returnsPayment()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.GetByIdAsync(payment.Id);

        response.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task GetByIdAsync_throwsNotFound()
    {
        var action = () => _service.GetByIdAsync(Guid.NewGuid());

        await action.Should().ThrowAsync<PaymentNotFoundException>();
    }

    [Fact]
    public async Task GetByOrderIdAsync_returnsPayment()
    {
        var payment = PaymentTestData.Payment(orderId: "order-by-id");
        _repository.Seed(payment);

        var response = await _service.GetByOrderIdAsync("order-by-id");

        response.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task ApproveAsync_WhenPending_ShouldApproveAndPublishPaymentApproved()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.ApproveAsync(
            payment.Id,
            PaymentTestData.ApproveRequest("provider-ref-1"));

        response.Status.Should().Be(PaymentStatus.Approved);
        response.Provider.Should().Be("SIMULATED");
        response.ProviderReference.Should().Be("provider-ref-1");
        response.ApprovedAt.Should().NotBeNull();
        _publisher.PaymentApprovedCalls.Should().Be(1);
        _publisher.LastApprovedPayment!.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task ApproveAsync_WhenPaymentNotPending_ShouldThrowAndNotPublish()
    {
        var payment = PaymentTestData.Payment(status: PaymentStatus.Rejected);
        _repository.Seed(payment);

        var action = () => _service.ApproveAsync(payment.Id, PaymentTestData.ApproveRequest());

        await action.Should().ThrowAsync<InvalidPaymentStatusTransitionException>();
        _publisher.PaymentApprovedCalls.Should().Be(0);
    }

    [Fact]
    public async Task RejectAsync_WhenPending_ShouldRejectAndPublishPaymentRejected()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.RejectAsync(
            payment.Id,
            PaymentTestData.RejectRequest("Card declined"));

        response.Status.Should().Be(PaymentStatus.Rejected);
        response.RejectionReason.Should().Be("Card declined");
        response.RejectedAt.Should().NotBeNull();
        _publisher.PaymentRejectedCalls.Should().Be(1);
        _publisher.LastRejectedPayment!.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task RejectAsync_WhenReasonIsEmpty_ShouldThrowAndNotPublish()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var action = () => _service.RejectAsync(payment.Id, new RejectPaymentRequest());

        await action.Should().ThrowAsync<FinanceValidationException>();
        _publisher.PaymentRejectedCalls.Should().Be(0);
    }

    [Fact]
    public async Task RejectAsync_WhenPaymentNotPending_ShouldThrowAndNotPublish()
    {
        var payment = PaymentTestData.Payment(status: PaymentStatus.Approved);
        _repository.Seed(payment);

        var action = () => _service.RejectAsync(payment.Id, PaymentTestData.RejectRequest());

        await action.Should().ThrowAsync<InvalidPaymentStatusTransitionException>();
        _publisher.PaymentRejectedCalls.Should().Be(0);
    }

    [Fact]
    public async Task CancelAsync_WhenPending_ShouldCancelAndPublishPaymentCancelled()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.CancelAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Cancelled);
        response.CancelledAt.Should().NotBeNull();
        _publisher.PaymentCancelledCalls.Should().Be(1);
        _publisher.LastCancelledPayment!.Id.Should().Be(payment.Id);
        _publisher.LastCancellationReason.Should().BeNull();
    }

    [Fact]
    public async Task CancelAsync_WhenPaymentNotPending_ShouldThrowAndNotPublish()
    {
        var payment = PaymentTestData.Payment(status: PaymentStatus.Approved);
        _repository.Seed(payment);

        var action = () => _service.CancelAsync(payment.Id);

        await action.Should().ThrowAsync<InvalidPaymentStatusTransitionException>();
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenOrderCancelledAndPaymentPending_ShouldCancelAndPublishPaymentCancelled()
    {
        var payment = PaymentTestData.Payment(orderId: "cancelled-order");
        _repository.Seed(payment);

        await _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("cancelled-order", "Cancelled"));

        var response = await _service.GetByIdAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Cancelled);
        _publisher.PaymentCancelledCalls.Should().Be(1);
        _publisher.LastCancelledPayment!.Id.Should().Be(payment.Id);
        _publisher.LastCancellationReason.Should().Be("ORDER_CANCELLED");
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenOrderCancelledAndPaymentMissing_ShouldNotThrowAndNotPublish()
    {
        var action = () => _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("missing-order", "Cancelled"));

        await action.Should().NotThrowAsync();
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenOrderCancelledAndPaymentApproved_ShouldNotChangeAndNotPublish()
    {
        var payment = PaymentTestData.Payment(orderId: "approved-order", status: PaymentStatus.Approved);
        _repository.Seed(payment);

        await _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("approved-order", "Cancelled"));

        var response = await _service.GetByIdAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Approved);
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenOrderCancelledAndPaymentAlreadyCancelled_ShouldNotPublishAgain()
    {
        var payment = PaymentTestData.Payment(orderId: "cancelled-order", status: PaymentStatus.Cancelled);
        _repository.Seed(payment);

        await _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("cancelled-order", "Cancelled"));

        var response = await _service.GetByIdAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Cancelled);
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenNewStatusIsNotCancelled_ShouldDoNothing()
    {
        var payment = PaymentTestData.Payment(orderId: "accepted-order");
        _repository.Seed(payment);

        await _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("accepted-order", "Accepted"));

        var response = await _service.GetByIdAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Pending);
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }
}
