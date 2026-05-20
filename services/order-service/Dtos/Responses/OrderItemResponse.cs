namespace OrderService.Dtos.Responses;

public sealed class OrderItemResponse
{
    public string ProductId { get; set; } = string.Empty;

    public string ProductNameSnapshot { get; set; } = string.Empty;

    public int Quantity { get; set; }

    public decimal UnitPriceSnapshot { get; set; }

    public List<SelectedModifierResponse> SelectedModifiers { get; set; } = [];

    public decimal Subtotal { get; set; }
}