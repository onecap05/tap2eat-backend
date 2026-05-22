namespace OrderService.Exceptions;

public sealed class CatalogServiceUnavailableException : OrderException
{
    public CatalogServiceUnavailableException()
        : base(
            "CATALOG_SERVICE_UNAVAILABLE",
            "Catalog service is unavailable.",
            StatusCodes.Status503ServiceUnavailable)
    {
    }
}
