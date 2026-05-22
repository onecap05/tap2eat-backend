namespace OrderService.Integrations.Catalog.Dtos;

public sealed class ValidateOrderResponse
{
    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public bool Valid { get; set; }

    public List<ValidatedOrderItemResponse> Items { get; set; } = [];

    public decimal Subtotal { get; set; }

    public decimal Total { get; set; }
}
