namespace OrderService.Dtos.Responses;

public sealed class PublicOrderTrackingItemResponse
{
    public string ProductNameSnapshot { get; set; } = string.Empty;

    public int Quantity { get; set; }

    public decimal UnitPriceSnapshot { get; set; }

    public List<PublicSelectedModifierResponse> SelectedModifiers { get; set; } = [];

    public decimal Subtotal { get; set; }
}
