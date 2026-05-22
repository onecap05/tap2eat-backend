namespace OrderService.Integrations.Catalog.Dtos;

public sealed class ValidateOrderRequest
{
    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public List<ValidateOrderItemRequest> Items { get; set; } = [];
}
