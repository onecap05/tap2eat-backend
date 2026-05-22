using System.Text.Json.Serialization;

namespace FinanceService.Messaging.Events;

public sealed class OrderCreatedEvent
{
    [JsonPropertyName("EventId")]
    public Guid EventId { get; set; }

    [JsonPropertyName("EventType")]
    public string EventType { get; set; } = "order.created";

    [JsonPropertyName("OrderId")]
    public string OrderId { get; set; } = string.Empty;

    [JsonPropertyName("CustomerAccountId")]
    public string CustomerAccountId { get; set; } = string.Empty;

    [JsonPropertyName("RestaurantId")]
    public string RestaurantId { get; set; } = string.Empty;

    [JsonPropertyName("BranchId")]
    public string BranchId { get; set; } = string.Empty;

    [JsonPropertyName("Subtotal")]
    public decimal Subtotal { get; set; }

    [JsonPropertyName("Total")]
    public decimal Total { get; set; }

    [JsonPropertyName("Status")]
    public string Status { get; set; } = string.Empty;

    [JsonPropertyName("CreatedAt")]
    public DateTime CreatedAt { get; set; }

    [JsonPropertyName("OccurredAt")]
    public DateTime OccurredAt { get; set; }
}
