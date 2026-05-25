using OrderService.Domain.Enums;

namespace OrderService.Dtos.Responses;

public sealed class PublicOrderTrackingResponse
{
    public string PublicTrackingCode { get; set; } = string.Empty;

    public string ShortOrderId { get; set; } = string.Empty;

    public OrderStatus Status { get; set; }

    public string? RestaurantNameSnapshot { get; set; }

    public string? BranchNameSnapshot { get; set; }

    public List<PublicOrderTrackingItemResponse> Items { get; set; } = [];

    public decimal Subtotal { get; set; }

    public decimal Total { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
