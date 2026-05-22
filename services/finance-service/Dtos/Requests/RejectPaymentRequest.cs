namespace FinanceService.Dtos.Requests;

public sealed class RejectPaymentRequest
{
    public string RejectionReason { get; set; } = string.Empty;
}
