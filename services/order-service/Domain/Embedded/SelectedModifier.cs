using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace OrderService.Domain.Embedded;

public sealed class SelectedModifier
{
    public string? ModifierGroupId { get; set; }

    public string ModifierGroupName { get; set; } = string.Empty;

    public string? ModifierOptionId { get; set; }

    public string ModifierOptionName { get; set; } = string.Empty;

    [BsonRepresentation(BsonType.Decimal128)]
    public decimal PriceAdjustment { get; set; }
}