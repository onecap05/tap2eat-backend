namespace OrderService.Messaging.Events;

public sealed class OrderStatusChangedEvent
{
    public Guid EventId { get; set; } = Guid.NewGuid();

    public string EventType { get; set; } = "order.status.changed";

    public string OrderId { get; set; } = string.Empty;

    public string CustomerAccountId { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public string PreviousStatus { get; set; } = string.Empty;

    public string NewStatus { get; set; } = string.Empty;

    public DateTime OccurredAt { get; set; } = DateTime.UtcNow;
}