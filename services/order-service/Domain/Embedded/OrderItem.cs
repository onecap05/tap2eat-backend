using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace OrderService.Domain.Embedded;

public sealed class OrderItem
{
    public string ProductId { get; set; } = string.Empty;

    public string ProductNameSnapshot { get; set; } = string.Empty;

    public int Quantity { get; set; }

    [BsonRepresentation(BsonType.Decimal128)]
    public decimal UnitPriceSnapshot { get; set; }

    public List<SelectedModifier> SelectedModifiers { get; set; } = [];

    [BsonRepresentation(BsonType.Decimal128)]
    public decimal Subtotal { get; set; }
}