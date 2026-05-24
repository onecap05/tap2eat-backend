using System.Globalization;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using FinanceService.Config;
using FinanceService.Exceptions;
using FinanceService.Services.Interfaces;
using Microsoft.Extensions.Options;

namespace FinanceService.Services.Implementations;

public sealed class PayPalClient : IPayPalClient
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly HttpClient _httpClient;
    private readonly PayPalSettings _settings;

    public PayPalClient(HttpClient httpClient, IOptions<PayPalSettings> settings)
    {
        _httpClient = httpClient;
        _settings = settings.Value;
        _httpClient.BaseAddress = new Uri($"{_settings.BaseUrl.TrimEnd('/')}/");
    }

    public async Task<string> CreateOrderAsync(
        decimal amount,
        string currency,
        string referenceId,
        CancellationToken cancellationToken = default)
    {
        var accessToken = await GetAccessTokenAsync(cancellationToken);
        using var request = new HttpRequestMessage(HttpMethod.Post, "v2/checkout/orders");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
        request.Content = JsonContent.Create(new
        {
            intent = "CAPTURE",
            purchase_units = new[]
            {
                new
                {
                    reference_id = referenceId,
                    amount = new
                    {
                        currency_code = currency,
                        value = amount.ToString("0.00", CultureInfo.InvariantCulture)
                    }
                }
            }
        });

        using var response = await _httpClient.SendAsync(request, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            throw new PayPalPaymentException(
                $"PayPal create order failed with status {(int)response.StatusCode}.");
        }

        var payload = await ReadJsonAsync<PayPalCreateOrderPayload>(response, cancellationToken);

        if (string.IsNullOrWhiteSpace(payload.Id))
        {
            throw new PayPalPaymentException("PayPal create order response did not include an order id.");
        }

        return payload.Id;
    }

    public async Task<PayPalCaptureResult> CaptureOrderAsync(
        string paypalOrderId,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(paypalOrderId))
        {
            throw new PayPalPaymentException("PayPal order id is required.", StatusCodes.Status400BadRequest);
        }

        var accessToken = await GetAccessTokenAsync(cancellationToken);
        using var request = new HttpRequestMessage(
            HttpMethod.Post,
            $"v2/checkout/orders/{Uri.EscapeDataString(paypalOrderId)}/capture");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
        request.Content = new StringContent("{}", Encoding.UTF8, "application/json");

        using var response = await _httpClient.SendAsync(request, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            throw new PayPalPaymentException(
                $"PayPal capture order failed with status {(int)response.StatusCode}.");
        }

        var payload = await ReadJsonAsync<PayPalCapturePayload>(response, cancellationToken);
        var capture = payload.PurchaseUnits?
            .SelectMany(unit => unit.Payments?.Captures ?? [])
            .FirstOrDefault();
        var status = payload.Status ?? capture?.Status ?? string.Empty;

        if (string.IsNullOrWhiteSpace(status))
        {
            throw new PayPalPaymentException("PayPal capture response did not include a status.");
        }

        return new PayPalCaptureResult(
            payload.Id ?? paypalOrderId,
            status,
            capture?.Id,
            capture?.Id ?? payload.Id ?? paypalOrderId);
    }

    private async Task<string> GetAccessTokenAsync(CancellationToken cancellationToken)
    {
        EnsureCredentialsConfigured();

        using var request = new HttpRequestMessage(HttpMethod.Post, "v1/oauth2/token");
        var credentials = Convert.ToBase64String(
            Encoding.UTF8.GetBytes($"{_settings.ClientId}:{_settings.ClientSecret}"));
        request.Headers.Authorization = new AuthenticationHeaderValue("Basic", credentials);
        request.Content = new FormUrlEncodedContent(new Dictionary<string, string>
        {
            ["grant_type"] = "client_credentials"
        });

        using var response = await _httpClient.SendAsync(request, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            throw new PayPalPaymentException(
                $"PayPal access token request failed with status {(int)response.StatusCode}.");
        }

        var payload = await ReadJsonAsync<PayPalTokenPayload>(response, cancellationToken);

        if (string.IsNullOrWhiteSpace(payload.AccessToken))
        {
            throw new PayPalPaymentException("PayPal access token response did not include a token.");
        }

        return payload.AccessToken;
    }

    private void EnsureCredentialsConfigured()
    {
        if (string.IsNullOrWhiteSpace(_settings.ClientId) ||
            string.IsNullOrWhiteSpace(_settings.ClientSecret))
        {
            throw new PayPalPaymentException("PayPal sandbox credentials are not configured.");
        }
    }

    private static async Task<TPayload> ReadJsonAsync<TPayload>(
        HttpResponseMessage response,
        CancellationToken cancellationToken)
    {
        try
        {
            var payload = await response.Content.ReadFromJsonAsync<TPayload>(
                JsonOptions,
                cancellationToken);

            return payload ?? throw new PayPalPaymentException("PayPal response body was empty.");
        }
        catch (JsonException exception)
        {
            throw new PayPalPaymentException($"PayPal response JSON was invalid: {exception.Message}");
        }
    }

    private sealed class PayPalTokenPayload
    {
        [JsonPropertyName("access_token")]
        public string? AccessToken { get; set; }
    }

    private sealed class PayPalCreateOrderPayload
    {
        public string? Id { get; set; }
    }

    private sealed class PayPalCapturePayload
    {
        public string? Id { get; set; }

        public string? Status { get; set; }

        [JsonPropertyName("purchase_units")]
        public IReadOnlyList<PayPalPurchaseUnitPayload>? PurchaseUnits { get; set; }
    }

    private sealed class PayPalPurchaseUnitPayload
    {
        public PayPalPaymentsPayload? Payments { get; set; }
    }

    private sealed class PayPalPaymentsPayload
    {
        public IReadOnlyList<PayPalCapturePayloadItem>? Captures { get; set; }
    }

    private sealed class PayPalCapturePayloadItem
    {
        public string? Id { get; set; }

        public string? Status { get; set; }
    }
}
