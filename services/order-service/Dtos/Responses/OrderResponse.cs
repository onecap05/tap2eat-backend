using OrderService.Domain.Enums;

namespace OrderService.Dtos.Responses;

public sealed class OrderResponse
{
    public string Id { get; set; } = string.Empty;

    public string CustomerAccountId { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public List<OrderItemResponse> Items { get; set; } = [];

    public decimal Subtotal { get; set; }

    public decimal Total { get; set; }

    public OrderStatus Status { get; set; }

    public string? Notes { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}