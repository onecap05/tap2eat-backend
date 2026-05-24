namespace FinanceService.Dtos.Requests;

public sealed class CapturePayPalOrderRequest
{
    public string PaypalOrderId { get; set; } = string.Empty;
}
