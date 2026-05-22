namespace OrderService.Messaging.Events;

public sealed class OrderCreatedEvent
{
    public Guid EventId { get; set; } = Guid.NewGuid();

    public string EventType { get; set; } = "order.created";

    public string OrderId { get; set; } = string.Empty;

    public string CustomerAccountId { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public decimal Subtotal { get; set; }

    public decimal Total { get; set; }

    public string Status { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; }

    public DateTime OccurredAt { get; set; } = DateTime.UtcNow;
}