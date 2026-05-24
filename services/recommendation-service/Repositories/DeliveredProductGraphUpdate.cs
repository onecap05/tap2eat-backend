namespace RecommendationService.Repositories;

public sealed class DeliveredProductGraphUpdate
{
    public string ProductId { get; set; } = string.Empty;

    public List<string> Tags { get; set; } = [];
}
