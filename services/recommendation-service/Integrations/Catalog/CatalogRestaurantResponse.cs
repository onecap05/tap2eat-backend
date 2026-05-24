namespace RecommendationService.Integrations.Catalog;

public sealed class CatalogRestaurantResponse
{
    public string Id { get; set; } = string.Empty;

    public string Name { get; set; } = string.Empty;

    public string? Description { get; set; }

    public CatalogImageResponse? Logo { get; set; }

    public bool? Active { get; set; }

    public bool? Open { get; set; }
}
