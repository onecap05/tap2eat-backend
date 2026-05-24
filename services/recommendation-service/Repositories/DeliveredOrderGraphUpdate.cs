namespace RecommendationService.Repositories;

public sealed class DeliveredOrderGraphUpdate
{
    public string CustomerAccountId { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public DateTime DeliveredAt { get; set; } = DateTime.UtcNow;

    public List<DeliveredProductGraphUpdate> Products { get; set; } = [];
}
