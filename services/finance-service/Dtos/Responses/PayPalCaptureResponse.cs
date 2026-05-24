using FinanceService.Domain.Enums;

namespace FinanceService.Dtos.Responses;

public sealed class PayPalCaptureResponse
{
    public Guid PaymentId { get; set; }

    public string PaypalOrderId { get; set; } = string.Empty;

    public string? CaptureId { get; set; }

    public PaymentStatus PaymentStatus { get; set; }

    public string? ProviderReference { get; set; }
}
