using System.ComponentModel.DataAnnotations;
using OrderService.Domain.Enums;

namespace OrderService.Dtos.Requests;

public sealed class CreateOrderRequest
{
    [Required]
    public string CustomerAccountId { get; set; } = string.Empty;

    [Required]
    public string RestaurantId { get; set; } = string.Empty;

    [Required]
    public string BranchId { get; set; } = string.Empty;

    [MinLength(1)]
    public List<CreateOrderItemRequest> Items { get; set; } = [];

    public string? Notes { get; set; }

    public PaymentMethod PaymentMethod { get; set; } = PaymentMethod.Online;

    public CashPaymentType? CashPaymentType { get; set; }

    public decimal? CashAmountProvided { get; set; }

    public decimal? EstimatedChange { get; set; }
}
