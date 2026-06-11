using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using OrderService.Domain.Embedded;
using OrderService.Domain.Enums;

namespace OrderService.Domain.Documents;

public sealed class OrderDocument
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string CustomerAccountId { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public string? PublicTrackingCode { get; set; }

    public List<OrderItem> Items { get; set; } = [];

    [BsonRepresentation(BsonType.Decimal128)]
    public decimal Subtotal { get; set; }

    [BsonRepresentation(BsonType.Decimal128)]
    public decimal Total { get; set; }

    [BsonRepresentation(BsonType.String)]
    public OrderStatus Status { get; set; } = OrderStatus.Created;

    [BsonRepresentation(BsonType.String)]
    public PaymentMethod PaymentMethod { get; set; } = PaymentMethod.Online;

    [BsonRepresentation(BsonType.String)]
    public CashPaymentType? CashPaymentType { get; set; }

    [BsonRepresentation(BsonType.Decimal128)]
    public decimal? CashAmountProvided { get; set; }

    [BsonRepresentation(BsonType.Decimal128)]
    public decimal? EstimatedChange { get; set; }

    public int? EstimatedPreparationMinutes { get; set; }

    public DateTime? EstimatedReadyAt { get; set; }

    public string? Notes { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
