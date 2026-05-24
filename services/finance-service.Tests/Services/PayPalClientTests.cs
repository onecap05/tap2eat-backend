using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using FinanceService.Config;
using FinanceService.Exceptions;
using FinanceService.Services.Implementations;
using FluentAssertions;
using Microsoft.Extensions.Options;

namespace FinanceService.Tests.Services;

public sealed class PayPalClientTests
{
    private const string ClientId = "paypal-client-id";
    private const string ClientSecret = "paypal-client-secret";

    [Fact]
    public async Task CreateOrderAsync_shouldRequestAccessTokenWithBasicAuth()
    {
        var handler = Handler(
            TokenResponse(),
            CreateOrderResponse());
        var client = CreateClient(handler);

        await client.CreateOrderAsync(123.45m, "MXN", "order-1");

        var tokenRequest = handler.Requests[0];
        tokenRequest.RequestUri!.PathAndQuery.Should().Be("/v1/oauth2/token");
        tokenRequest.Headers.Authorization!.Scheme.Should().Be("Basic");
        tokenRequest.Headers.Authorization.Parameter.Should().Be(
            Convert.ToBase64String(Encoding.UTF8.GetBytes($"{ClientId}:{ClientSecret}")));
        (await tokenRequest.Content!.ReadAsStringAsync()).Should().Be("grant_type=client_credentials");
    }

    [Fact]
    public async Task CreateOrderAsync_shouldReturnPayPalOrderId()
    {
        var client = CreateClient(Handler(TokenResponse(), CreateOrderResponse("PAYPAL-123")));

        var paypalOrderId = await client.CreateOrderAsync(50m, "MXN", "order-1");

        paypalOrderId.Should().Be("PAYPAL-123");
    }

    [Fact]
    public async Task CreateOrderAsync_shouldSendCaptureIntentAndAmount()
    {
        var handler = Handler(TokenResponse(), CreateOrderResponse());
        var client = CreateClient(handler);

        await client.CreateOrderAsync(50.5m, "MXN", "order-1");

        var body = await handler.Requests[1].Content!.ReadAsStringAsync();
        body.Should().Contain("\"intent\":\"CAPTURE\"");
        body.Should().Contain("\"currency_code\":\"MXN\"");
        body.Should().Contain("\"value\":\"50.50\"");
        body.Should().Contain("\"reference_id\":\"order-1\"");
    }

    [Fact]
    public async Task CreateOrderAsync_whenTokenRequestFails_shouldThrowSafeException()
    {
        var client = CreateClient(Handler(new HttpResponseMessage(HttpStatusCode.Unauthorized)));

        var act = () => client.CreateOrderAsync(50m, "MXN", "order-1");

        var exception = await act.Should().ThrowAsync<PayPalPaymentException>();
        exception.Which.Message.Should().Contain("401");
        exception.Which.Message.Should().NotContain(ClientSecret);
    }

    [Fact]
    public async Task CreateOrderAsync_whenTokenJsonIsInvalid_shouldThrowSafeException()
    {
        var client = CreateClient(Handler(TextResponse("not-json")));

        var act = () => client.CreateOrderAsync(50m, "MXN", "order-1");

        var exception = await act.Should().ThrowAsync<PayPalPaymentException>();
        exception.Which.Message.Should().Contain("JSON");
        exception.Which.Message.Should().NotContain(ClientSecret);
    }

    [Fact]
    public async Task CreateOrderAsync_whenPayPalErrors_shouldThrowSafeException()
    {
        var client = CreateClient(Handler(
            TokenResponse(),
            new HttpResponseMessage(HttpStatusCode.InternalServerError)));

        var act = () => client.CreateOrderAsync(50m, "MXN", "order-1");

        var exception = await act.Should().ThrowAsync<PayPalPaymentException>();
        exception.Which.Message.Should().Contain("500");
        exception.Which.Message.Should().NotContain(ClientSecret);
    }

    [Fact]
    public async Task CaptureOrderAsync_whenCompleted_shouldReturnCaptureResult()
    {
        var client = CreateClient(Handler(
            TokenResponse(),
            CaptureResponse("COMPLETED", "CAPTURE-1")));

        var result = await client.CaptureOrderAsync("PAYPAL-ORDER-1");

        result.PayPalOrderId.Should().Be("PAYPAL-ORDER-1");
        result.Status.Should().Be("COMPLETED");
        result.CaptureId.Should().Be("CAPTURE-1");
        result.ProviderReference.Should().Be("CAPTURE-1");
    }

    [Fact]
    public async Task CaptureOrderAsync_whenDeclined_shouldReturnControlledStatus()
    {
        var client = CreateClient(Handler(
            TokenResponse(),
            CaptureResponse("DECLINED", "CAPTURE-1")));

        var result = await client.CaptureOrderAsync("PAYPAL-ORDER-1");

        result.Status.Should().Be("DECLINED");
    }

    [Fact]
    public async Task CaptureOrderAsync_whenJsonIsInvalid_shouldThrowSafeException()
    {
        var client = CreateClient(Handler(TokenResponse(), TextResponse("not-json")));

        var act = () => client.CaptureOrderAsync("PAYPAL-ORDER-1");

        var exception = await act.Should().ThrowAsync<PayPalPaymentException>();
        exception.Which.Message.Should().Contain("JSON");
        exception.Which.Message.Should().NotContain(ClientSecret);
    }

    private static PayPalClient CreateClient(FakePayPalHttpMessageHandler handler)
    {
        return new PayPalClient(
            new HttpClient(handler),
            Options.Create(new PayPalSettings
            {
                BaseUrl = "https://paypal.test",
                ClientId = ClientId,
                ClientSecret = ClientSecret,
                Currency = "MXN"
            }));
    }

    private static FakePayPalHttpMessageHandler Handler(params HttpResponseMessage[] responses)
    {
        return new FakePayPalHttpMessageHandler(responses);
    }

    private static HttpResponseMessage TokenResponse()
    {
        return JsonResponse(new { access_token = "access-token" });
    }

    private static HttpResponseMessage CreateOrderResponse(string id = "PAYPAL-ORDER-1")
    {
        return JsonResponse(new { id, status = "CREATED" }, HttpStatusCode.Created);
    }

    private static HttpResponseMessage CaptureResponse(string status, string captureId)
    {
        return JsonResponse(new
        {
            id = "PAYPAL-ORDER-1",
            status,
            purchase_units = new[]
            {
                new
                {
                    payments = new
                    {
                        captures = new[]
                        {
                            new { id = captureId, status }
                        }
                    }
                }
            }
        });
    }

    private static HttpResponseMessage JsonResponse(object value, HttpStatusCode statusCode = HttpStatusCode.OK)
    {
        return TextResponse(JsonSerializer.Serialize(value), statusCode, "application/json");
    }

    private static HttpResponseMessage TextResponse(
        string value,
        HttpStatusCode statusCode = HttpStatusCode.OK,
        string contentType = "text/plain")
    {
        return new HttpResponseMessage(statusCode)
        {
            Content = new StringContent(value, Encoding.UTF8, contentType)
        };
    }

    private sealed class FakePayPalHttpMessageHandler : HttpMessageHandler
    {
        private readonly Queue<HttpResponseMessage> _responses;

        public FakePayPalHttpMessageHandler(IEnumerable<HttpResponseMessage> responses)
        {
            _responses = new Queue<HttpResponseMessage>(responses);
        }

        public List<HttpRequestMessage> Requests { get; } = [];

        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request,
            CancellationToken cancellationToken)
        {
            Requests.Add(CloneRequest(request));

            return Task.FromResult(_responses.Dequeue());
        }

        private static HttpRequestMessage CloneRequest(HttpRequestMessage request)
        {
            var clone = new HttpRequestMessage(request.Method, request.RequestUri);

            foreach (var header in request.Headers)
            {
                clone.Headers.TryAddWithoutValidation(header.Key, header.Value);
            }

            if (request.Content is not null)
            {
                var body = request.Content.ReadAsStringAsync().GetAwaiter().GetResult();
                clone.Content = new StringContent(body);

                foreach (var header in request.Content.Headers)
                {
                    clone.Content.Headers.TryAddWithoutValidation(header.Key, header.Value);
                }
            }

            return clone;
        }
    }
}
