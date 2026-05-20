using System.ComponentModel.DataAnnotations;

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
}