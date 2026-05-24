using RecommendationService.Messaging.Events;

namespace RecommendationService.Messaging.Consumers;

public interface IOrderDeliveredEventProcessor
{
    Task ProcessAsync(
        OrderStatusChangedEvent orderStatusChangedEvent,
        CancellationToken cancellationToken = default);

    Task ProcessRawAsync(
        string rawMessage,
        CancellationToken cancellationToken = default);
}
