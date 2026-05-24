using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using FinanceService.Data;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Services.Interfaces;
using FinanceService.Tests.TestData;
using FluentAssertions;
using Microsoft.Extensions.DependencyInjection;

namespace FinanceService.Tests.Integration;

public sealed class PaymentsControllerIntegrationTests : IClassFixture<FinanceApiTestFixture>, IAsyncLifetime
{
    private const string SimulationToken = "tap2eat-payment-dev-token";

    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        Converters = { new JsonStringEnumConverter() }
    };

    private readonly FinanceApiTestFixture _fixture;

    public PaymentsControllerIntegrationTests(FinanceApiTestFixture fixture)
    {
        _fixture = fixture;
    }

    public Task InitializeAsync()
    {
        return _fixture.ClearPaymentsAsync();
    }

    public Task DisposeAsync()
    {
        return Task.CompletedTask;
    }

    [Fact]
    public async Task Health_returnsUp()
    {
        var response = await _fixture.Client.GetAsync("/api/finance/health");

        response.StatusCode.Should().Be(HttpStatusCode.OK);

        var content = await response.Content.ReadAsStringAsync();
        content.Should().Contain("UP");
    }

    [Fact]
    public async Task GetByOrderId_returnsSeededPayment()
    {
        var payment = await CreatePaymentAsync("order-get");

        var response = await _fixture.Client.GetAsync($"/api/payments/order/{payment.OrderId}");

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var body = await response.Content.ReadFromJsonAsync<PaymentResponse>(JsonOptions);
        body!.Id.Should().Be(payment.Id);
        body.Status.Should().Be(PaymentStatus.Pending);
    }

    [Fact]
    public async Task Approve_persistsApprovedInPostgreSql()
    {
        var payment = await CreatePaymentAsync("order-approve");

        var response = await PatchSimulationAsJsonAsync(
            $"/api/payments/{payment.Id}/approve",
            new ApprovePaymentRequest { ProviderReference = "manual-ref" });

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var body = await response.Content.ReadFromJsonAsync<PaymentResponse>(JsonOptions);
        body!.Status.Should().Be(PaymentStatus.Approved);

        await using var scope = _fixture.Services.CreateAsyncScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<FinanceDbContext>();
        var persistedPayment = await dbContext.Payments.FindAsync(payment.Id);
        persistedPayment!.Status.Should().Be(PaymentStatus.Approved);
    }

    [Fact]
    public async Task Approve_withoutSimulationToken_returnsForbidden()
    {
        var payment = await CreatePaymentAsync("order-approve-forbidden");

        var response = await _fixture.Client.PatchAsJsonAsync(
            $"/api/payments/{payment.Id}/approve",
            new ApprovePaymentRequest());

        response.StatusCode.Should().Be(HttpStatusCode.Forbidden);
    }

    [Fact]
    public async Task Reject_persistsRejectedInPostgreSql()
    {
        var payment = await CreatePaymentAsync("order-reject");

        var response = await PatchSimulationAsJsonAsync(
            $"/api/payments/{payment.Id}/reject",
            new RejectPaymentRequest { RejectionReason = "Declined" });

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var body = await response.Content.ReadFromJsonAsync<PaymentResponse>(JsonOptions);
        body!.Status.Should().Be(PaymentStatus.Rejected);
        body.RejectionReason.Should().Be("Declined");
    }

    [Fact]
    public async Task Cancel_persistsCancelledInPostgreSql()
    {
        var payment = await CreatePaymentAsync("order-cancel");

        var response = await PatchSimulationAsync($"/api/payments/{payment.Id}/cancel");

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var body = await response.Content.ReadFromJsonAsync<PaymentResponse>(JsonOptions);
        body!.Status.Should().Be(PaymentStatus.Cancelled);
    }

    [Fact]
    public async Task GetMissingPayment_returnsNotFound()
    {
        var response = await _fixture.Client.GetAsync($"/api/payments/{Guid.NewGuid()}");

        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
    }

    [Fact]
    public async Task InvalidTransition_returnsConflict()
    {
        var payment = await CreatePaymentAsync("order-conflict");

        await PatchSimulationAsJsonAsync(
            $"/api/payments/{payment.Id}/approve",
            new ApprovePaymentRequest());

        var response = await PatchSimulationAsync($"/api/payments/{payment.Id}/cancel");

        response.StatusCode.Should().Be(HttpStatusCode.Conflict);
    }

    private async Task<PaymentResponse> CreatePaymentAsync(string orderId)
    {
        await using var scope = _fixture.Services.CreateAsyncScope();
        var paymentService = scope.ServiceProvider.GetRequiredService<IPaymentService>();

        return await paymentService.CreatePendingPaymentFromOrderAsync(
            PaymentTestData.OrderCreatedEvent(orderId));
    }

    private Task<HttpResponseMessage> PatchSimulationAsync(string requestUri)
    {
        var request = new HttpRequestMessage(HttpMethod.Patch, requestUri);
        request.Headers.Add("X-Simulated-Payment-Token", SimulationToken);

        return _fixture.Client.SendAsync(request);
    }

    private Task<HttpResponseMessage> PatchSimulationAsJsonAsync<TRequest>(
        string requestUri,
        TRequest requestBody)
    {
        var request = new HttpRequestMessage(HttpMethod.Patch, requestUri)
        {
            Content = JsonContent.Create(requestBody)
        };
        request.Headers.Add("X-Simulated-Payment-Token", SimulationToken);

        return _fixture.Client.SendAsync(request);
    }
}
