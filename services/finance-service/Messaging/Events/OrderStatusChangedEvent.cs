using System.Text.Json.Serialization;

namespace FinanceService.Messaging.Events;

public sealed class OrderStatusChangedEvent
{
    [JsonPropertyName("EventId")]
    public Guid EventId { get; set; }

    [JsonPropertyName("EventType")]
    public string EventType { get; set; } = "order.status.changed";

    [JsonPropertyName("OrderId")]
    public string OrderId { get; set; } = string.Empty;

    [JsonPropertyName("CustomerAccountId")]
    public string CustomerAccountId { get; set; } = string.Empty;

    [JsonPropertyName("RestaurantId")]
    public string RestaurantId { get; set; } = string.Empty;

    [JsonPropertyName("BranchId")]
    public string BranchId { get; set; } = string.Empty;

    [JsonPropertyName("PreviousStatus")]
    public string PreviousStatus { get; set; } = string.Empty;

    [JsonPropertyName("NewStatus")]
    public string NewStatus { get; set; } = string.Empty;

    [JsonPropertyName("OccurredAt")]
    public DateTime OccurredAt { get; set; }
}
