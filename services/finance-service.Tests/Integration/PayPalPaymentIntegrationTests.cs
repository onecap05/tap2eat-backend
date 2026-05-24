using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Exceptions;
using FinanceService.Services.Interfaces;
using FinanceService.Tests.TestData;
using FluentAssertions;
using Microsoft.Extensions.DependencyInjection;

namespace FinanceService.Tests.Integration;

public sealed class PayPalPaymentIntegrationTests : IClassFixture<FinanceApiTestFixture>, IAsyncLifetime
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        Converters = { new JsonStringEnumConverter() }
    };

    private readonly FinanceApiTestFixture _fixture;

    public PayPalPaymentIntegrationTests(FinanceApiTestFixture fixture)
    {
        _fixture = fixture;
    }

    public async Task InitializeAsync()
    {
        await _fixture.ClearPaymentsAsync();
        _fixture.PayPalClient.Reset();
        _fixture.PaymentEventPublisher.Reset();
    }

    public Task DisposeAsync()
    {
        return Task.CompletedTask;
    }

    [Fact]
    public async Task PayPalFlow_shouldCreateOrderCaptureAndApprovePayment()
    {
        var payment = await CreatePaymentAsync("paypal-flow");

        var createResponse = await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/create-order",
            new CreatePayPalOrderRequest());
        var captureResponse = await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/capture",
            new CapturePayPalOrderRequest { PaypalOrderId = "PAYPAL-ORDER-1" });

        createResponse.StatusCode.Should().Be(HttpStatusCode.OK);
        captureResponse.StatusCode.Should().Be(HttpStatusCode.OK);

        var captureBody = await captureResponse.Content.ReadFromJsonAsync<PayPalCaptureResponse>(JsonOptions);
        captureBody!.PaymentStatus.Should().Be(PaymentStatus.Approved);
        captureBody.ProviderReference.Should().Be("CAPTURE-1");
    }

    [Fact]
    public async Task CapturePayPalOrder_whenCompleted_shouldPublishPaymentApproved()
    {
        var payment = await CreatePaymentAsync("paypal-publish");
        await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/create-order",
            new CreatePayPalOrderRequest());

        var response = await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/capture",
            new CapturePayPalOrderRequest { PaypalOrderId = "PAYPAL-ORDER-1" });

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        _fixture.PaymentEventPublisher.PaymentApprovedCalls.Should().Be(1);
        _fixture.PaymentEventPublisher.LastApprovedPayment!.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task CapturePayPalOrder_whenPayPalFails_shouldNotPublishPaymentApproved()
    {
        var payment = await CreatePaymentAsync("paypal-fail");
        _fixture.PayPalClient.CaptureOrderException = new PayPalPaymentException("PayPal failed.");

        var response = await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/capture",
            new CapturePayPalOrderRequest { PaypalOrderId = "PAYPAL-ORDER-1" });

        response.StatusCode.Should().Be(HttpStatusCode.BadGateway);
        _fixture.PaymentEventPublisher.PaymentApprovedCalls.Should().Be(0);
    }

    [Fact]
    public async Task CreatePayPalOrder_whenPaymentMissing_shouldReturnNotFound()
    {
        var response = await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{Guid.NewGuid()}/paypal/create-order",
            new CreatePayPalOrderRequest());

        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
    }

    [Fact]
    public async Task CreatePayPalOrder_whenPaymentApproved_shouldReturnConflict()
    {
        var payment = await CreatePaymentAsync("paypal-create-conflict");
        await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/capture",
            new CapturePayPalOrderRequest { PaypalOrderId = "PAYPAL-ORDER-1" });

        var response = await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/create-order",
            new CreatePayPalOrderRequest());

        response.StatusCode.Should().Be(HttpStatusCode.Conflict);
    }

    [Fact]
    public async Task CapturePayPalOrder_whenPaymentMissing_shouldReturnNotFound()
    {
        var response = await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{Guid.NewGuid()}/paypal/capture",
            new CapturePayPalOrderRequest { PaypalOrderId = "PAYPAL-ORDER-1" });

        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
    }

    [Fact]
    public async Task GetByOrderId_afterCapture_shouldReturnApprovedPayment()
    {
        var payment = await CreatePaymentAsync("paypal-get-approved");
        await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/create-order",
            new CreatePayPalOrderRequest());
        await _fixture.Client.PostAsJsonAsync(
            $"/api/payments/{payment.Id}/paypal/capture",
            new CapturePayPalOrderRequest { PaypalOrderId = "PAYPAL-ORDER-1" });

        var response = await _fixture.Client.GetAsync($"/api/payments/order/{payment.OrderId}");

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var body = await response.Content.ReadFromJsonAsync<PaymentResponse>(JsonOptions);
        body!.Status.Should().Be(PaymentStatus.Approved);
    }

    private async Task<PaymentResponse> CreatePaymentAsync(string orderId)
    {
        await using var scope = _fixture.Services.CreateAsyncScope();
        var paymentService = scope.ServiceProvider.GetRequiredService<IPaymentService>();

        return await paymentService.CreatePendingPaymentFromOrderAsync(
            PaymentTestData.OrderCreatedEvent(orderId));
    }
}
