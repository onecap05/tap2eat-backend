using OrderService.Dtos.Responses;
using OrderService.Messaging.Publishers;

namespace OrderService.Tests.Fakes;

public sealed class FakeOrderEventPublisher : IOrderEventPublisher
{
    public int OrderCreatedCalls { get; private set; }

    public int OrderStatusChangedCalls { get; private set; }

    public OrderResponse? LastCreatedOrder { get; private set; }

    public OrderResponse? LastStatusChangedOrder { get; private set; }

    public string? LastPreviousStatus { get; private set; }

    public Task PublishOrderCreatedAsync(
        OrderResponse order,
        CancellationToken cancellationToken = default)
    {
        OrderCreatedCalls++;
        LastCreatedOrder = order;

        return Task.CompletedTask;
    }

    public Task PublishOrderStatusChangedAsync(
        OrderResponse order,
        string previousStatus,
        CancellationToken cancellationToken = default)
    {
        OrderStatusChangedCalls++;
        LastStatusChangedOrder = order;
        LastPreviousStatus = previousStatus;

        return Task.CompletedTask;
    }
}