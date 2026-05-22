namespace OrderService.Exceptions;

public sealed class OrderValidationException : OrderException
{
    public OrderValidationException(string message)
        : base("ORDER_VALIDATION_ERROR", message, StatusCodes.Status400BadRequest)
    {
    }
}
