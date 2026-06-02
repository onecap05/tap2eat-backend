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

    public int? EstimatedPreparationMinutes { get; set; }

    public DateTime? EstimatedReadyAt { get; set; }

    public List<OrderStatusChangedItemEvent> Items { get; set; } = [];

    public DateTime OccurredAt { get; set; } = DateTime.UtcNow;
}

public sealed class OrderStatusChangedItemEvent
{
    public string ProductId { get; set; } = string.Empty;

    public int Quantity { get; set; }

    public string? ProductNameSnapshot { get; set; }
}
