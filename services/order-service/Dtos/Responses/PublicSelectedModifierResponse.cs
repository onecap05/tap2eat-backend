namespace OrderService.Dtos.Responses;

public sealed class PublicSelectedModifierResponse
{
    public string ModifierGroupName { get; set; } = string.Empty;

    public string ModifierOptionName { get; set; } = string.Empty;

    public decimal PriceAdjustment { get; set; }
}
