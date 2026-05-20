namespace OrderService.Domain.Enums;

public enum OrderStatus
{
    Created,
    Accepted,
    Preparing,
    Ready,
    Delivered,
    Cancelled
}