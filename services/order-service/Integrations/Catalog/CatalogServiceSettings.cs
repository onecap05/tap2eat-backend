namespace OrderService.Integrations.Catalog;

public sealed class CatalogServiceSettings
{
    public const string SectionName = "CatalogService";

    public string BaseUrl { get; set; } = string.Empty;

    public string InternalServiceToken { get; set; } = string.Empty;
}