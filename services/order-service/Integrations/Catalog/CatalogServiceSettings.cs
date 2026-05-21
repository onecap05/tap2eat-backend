namespace OrderService.Integrations.Catalog;

public sealed class CatalogServiceSettings
{
    public const string SectionName = "CatalogService";

    public string BaseUrl { get; set; } = "http://localhost:8082";

    public string InternalServiceToken { get; set; } = "tap2eat-internal-dev-token";
}
