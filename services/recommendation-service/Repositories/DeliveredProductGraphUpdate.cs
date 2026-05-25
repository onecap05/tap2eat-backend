namespace RecommendationService.Repositories;

public sealed class DeliveredProductGraphUpdate
{
    public string ProductId { get; set; } = string.Empty;

    public int Quantity { get; set; } = 1;

    public string? ProductNameSnapshot { get; set; }

    public List<string> Tags { get; set; } = [];
}
