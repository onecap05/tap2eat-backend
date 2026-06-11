using System.Text.Json.Serialization;

namespace FinanceService.Messaging.Events;

public sealed class PaymentApprovedEvent
{
    [JsonPropertyName("EventId")]
    public Guid EventId { get; set; } = Guid.NewGuid();

    [JsonPropertyName("EventType")]
    public string EventType { get; set; } = "payment.approved";

    [JsonPropertyName("PaymentId")]
    public Guid PaymentId { get; set; }

    [JsonPropertyName("OrderId")]
    public string OrderId { get; set; } = string.Empty;

    [JsonPropertyName("CustomerAccountId")]
    public string CustomerAccountId { get; set; } = string.Empty;

    [JsonPropertyName("RestaurantId")]
    public string RestaurantId { get; set; } = string.Empty;

    [JsonPropertyName("BranchId")]
    public string BranchId { get; set; } = string.Empty;

    [JsonPropertyName("Amount")]
    public decimal Amount { get; set; }

    [JsonPropertyName("Currency")]
    public string Currency { get; set; } = string.Empty;

    [JsonPropertyName("Status")]
    public string Status { get; set; } = string.Empty;

    [JsonPropertyName("Provider")]
    public string? Provider { get; set; }

    [JsonPropertyName("ProviderReference")]
    public string? ProviderReference { get; set; }

    [JsonPropertyName("AmountReceived")]
    public decimal? AmountReceived { get; set; }

    [JsonPropertyName("ChangeAmount")]
    public decimal? ChangeAmount { get; set; }

    [JsonPropertyName("OccurredAt")]
    public DateTime OccurredAt { get; set; } = DateTime.UtcNow;
}
