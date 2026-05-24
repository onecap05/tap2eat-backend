namespace RecommendationService.Integrations.Catalog;

public sealed class CatalogBranchResponse
{
    public string Id { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string Name { get; set; } = string.Empty;

    public string? FormattedAddress { get; set; }

    public double? Latitude { get; set; }

    public double? Longitude { get; set; }

    public bool? IsMainBranch { get; set; }

    public bool? Active { get; set; }

    public bool? Open { get; set; }
}
