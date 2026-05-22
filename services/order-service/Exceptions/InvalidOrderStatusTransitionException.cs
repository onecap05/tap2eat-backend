using OrderService.Domain.Enums;

namespace OrderService.Exceptions;

public sealed class InvalidOrderStatusTransitionException : OrderException
{
    public InvalidOrderStatusTransitionException(OrderStatus currentStatus, OrderStatus requestedStatus)
        : base(
            "INVALID_ORDER_STATUS_TRANSITION",
            $"Order status cannot change from {currentStatus} to {requestedStatus}.",
            StatusCodes.Status409Conflict)
    {
    }
}
