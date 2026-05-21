namespace OrderService.Integrations.Catalog.Dtos;

public sealed class ValidatedOrderItemResponse
{
    public string ProductId { get; set; } = string.Empty;

    public string ProductName { get; set; } = string.Empty;

    public int Quantity { get; set; }

    public decimal UnitPrice { get; set; }

    public List<ValidatedModifierResponse> SelectedModifiers { get; set; } = [];

    public decimal Subtotal { get; set; }
}
