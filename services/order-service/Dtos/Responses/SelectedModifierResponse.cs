namespace OrderService.Dtos.Responses;

public sealed class SelectedModifierResponse
{
    public string? ModifierGroupId { get; set; }

    public string ModifierGroupName { get; set; } = string.Empty;

    public string? ModifierOptionId { get; set; }

    public string ModifierOptionName { get; set; } = string.Empty;

    public decimal PriceAdjustment { get; set; }
}