namespace RecommendationService.Integrations.Catalog;

public sealed class CatalogProductResponse
{
    public string Id { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string? CategoryId { get; set; }

    public string Name { get; set; } = string.Empty;

    public decimal Price { get; set; }

    public CatalogImageResponse? Image { get; set; }

    public bool? Active { get; set; }

    public bool? Available { get; set; }

    public List<string> Tags { get; set; } = [];

    public List<string> DietaryFlags { get; set; } = [];

    public List<string> Allergens { get; set; } = [];
}
