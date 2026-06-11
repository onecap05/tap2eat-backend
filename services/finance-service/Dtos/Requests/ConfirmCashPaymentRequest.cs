using System.ComponentModel.DataAnnotations;

namespace FinanceService.Dtos.Requests;

public sealed class ConfirmCashPaymentRequest
{
    [Required]
    public decimal? AmountReceived { get; set; }
}
