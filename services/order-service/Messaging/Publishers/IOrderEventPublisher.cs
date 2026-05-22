using OrderService.Dtos.Responses;

namespace OrderService.Messaging.Publishers;

public interface IOrderEventPublisher
{
    Task PublishOrderCreatedAsync(OrderResponse order, CancellationToken cancellationToken = default);

    Task PublishOrderStatusChangedAsync(
        OrderResponse order,
        string previousStatus,
        CancellationToken cancellationToken = default);
}