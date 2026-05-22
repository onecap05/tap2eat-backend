using OrderService.Domain.Enums;

namespace OrderService.Dtos.Requests;

public sealed class OrderQueryRequest
{
    public OrderStatus? Status { get; set; }

    public DateTime? From { get; set; }

    public DateTime? To { get; set; }
}
