namespace OrderService.Integrations.Catalog.Dtos;

public sealed class ValidateOrderItemRequest
{
    public string ProductId { get; set; } = string.Empty;

    public int Quantity { get; set; }

    public List<string> SelectedModifierOptionIds { get; set; } = [];
}
