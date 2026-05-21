namespace OrderService.Exceptions;

public sealed class CatalogValidationException : OrderException
{
    public CatalogValidationException(string message)
        : base("CATALOG_VALIDATION_FAILED", message, StatusCodes.Status400BadRequest)
    {
    }
}
