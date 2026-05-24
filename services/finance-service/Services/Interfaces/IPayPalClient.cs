namespace FinanceService.Services.Interfaces;

public interface IPayPalClient
{
    Task<string> CreateOrderAsync(
        decimal amount,
        string currency,
        string referenceId,
        CancellationToken cancellationToken = default);

    Task<PayPalCaptureResult> CaptureOrderAsync(
        string paypalOrderId,
        CancellationToken cancellationToken = default);
}

public sealed record PayPalCaptureResult(
    string PayPalOrderId,
    string Status,
    string? CaptureId,
    string? ProviderReference);
