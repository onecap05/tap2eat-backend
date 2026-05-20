namespace OrderService.Exceptions;

public sealed class OrderNotFoundException : OrderException
{
    public OrderNotFoundException(string id)
        : base("ORDER_NOT_FOUND", $"Order '{id}' was not found.", StatusCodes.Status404NotFound)
    {
    }
}
