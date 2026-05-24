namespace FinanceService.Dtos.Responses;

public sealed class PayPalOrderResponse
{
    public Guid PaymentId { get; set; }

    public string PaypalOrderId { get; set; } = string.Empty;

    public string Status { get; set; } = string.Empty;

    public decimal Amount { get; set; }

    public string Currency { get; set; } = string.Empty;
}
