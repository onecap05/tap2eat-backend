namespace RecommendationService.Config;

public sealed class ExternalServiceSettings
{
    public const string SectionName = "ExternalServices";

    public string CatalogServiceBaseUrl { get; set; } = "http://localhost:8082";
}
