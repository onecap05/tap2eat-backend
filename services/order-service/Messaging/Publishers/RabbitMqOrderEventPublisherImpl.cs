using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Options;
using OrderService.Config;
using OrderService.Dtos.Responses;
using OrderService.Messaging.Events;
using RabbitMQ.Client;

namespace OrderService.Messaging.Publishers;

public sealed class RabbitMqOrderEventPublisherImpl : IOrderEventPublisher
{
    private readonly RabbitMqSettings _settings;
    private readonly ILogger<RabbitMqOrderEventPublisherImpl> _logger;

    public RabbitMqOrderEventPublisherImpl(
        IOptions<RabbitMqSettings> options,
        ILogger<RabbitMqOrderEventPublisherImpl> logger)
    {
        _settings = options.Value;
        _logger = logger;
    }

    public async Task PublishOrderCreatedAsync(
        OrderResponse order,
        CancellationToken cancellationToken = default)
    {
        var message = new OrderCreatedEvent
        {
            OrderId = order.Id,
            CustomerAccountId = order.CustomerAccountId,
            RestaurantId = order.RestaurantId,
            BranchId = order.BranchId,
            Subtotal = order.Subtotal,
            Total = order.Total,
            Status = order.Status.ToString(),
            CreatedAt = order.CreatedAt
        };

        await PublishAsync("order.created", message, cancellationToken);
    }

    public async Task PublishOrderStatusChangedAsync(
        OrderResponse order,
        string previousStatus,
        CancellationToken cancellationToken = default)
    {
        var message = CreateOrderStatusChangedEvent(order, previousStatus);

        await PublishAsync("order.status.changed", message, cancellationToken);
    }

    public static OrderStatusChangedEvent CreateOrderStatusChangedEvent(
        OrderResponse order,
        string previousStatus)
    {
        var newStatus = order.Status.ToString();

        return new OrderStatusChangedEvent
        {
            OrderId = order.Id,
            CustomerAccountId = order.CustomerAccountId,
            RestaurantId = order.RestaurantId,
            BranchId = order.BranchId,
            PreviousStatus = previousStatus,
            NewStatus = newStatus,
            Items = string.Equals(newStatus, "Delivered", StringComparison.OrdinalIgnoreCase)
                ? order.Items
                    .Where(item => !string.IsNullOrWhiteSpace(item.ProductId))
                    .Select(item => new OrderStatusChangedItemEvent
                    {
                        ProductId = item.ProductId,
                        Quantity = item.Quantity,
                        ProductNameSnapshot = item.ProductNameSnapshot
                    })
                    .ToList()
                : []
        };
    }

    private async Task PublishAsync<T>(
        string routingKey,
        T message,
        CancellationToken cancellationToken)
    {
        if (!_settings.Enabled)
        {
            _logger.LogInformation(
                "RabbitMQ publishing disabled. Event {RoutingKey} was not published.",
                routingKey);

            return;
        }

        var factory = new ConnectionFactory
        {
            HostName = _settings.HostName,
            Port = _settings.Port,
            UserName = _settings.UserName,
            Password = _settings.Password
        };

        await using var connection = await factory.CreateConnectionAsync(cancellationToken);
        await using var channel = await connection.CreateChannelAsync(cancellationToken: cancellationToken);

        await channel.ExchangeDeclareAsync(
            exchange: _settings.ExchangeName,
            type: _settings.ExchangeType,
            durable: true,
            autoDelete: false,
            arguments: null,
            cancellationToken: cancellationToken);

        var json = JsonSerializer.Serialize(message);
        var body = Encoding.UTF8.GetBytes(json);

        var properties = new BasicProperties
        {
            ContentType = "application/json",
            DeliveryMode = DeliveryModes.Persistent,
            MessageId = Guid.NewGuid().ToString(),
            Timestamp = new AmqpTimestamp(DateTimeOffset.UtcNow.ToUnixTimeSeconds())
        };

        await channel.BasicPublishAsync(
            exchange: _settings.ExchangeName,
            routingKey: routingKey,
            mandatory: false,
            basicProperties: properties,
            body: body,
            cancellationToken: cancellationToken);

        _logger.LogInformation(
            "Published RabbitMQ event {RoutingKey} for exchange {ExchangeName}.",
            routingKey,
            _settings.ExchangeName);
    }
}
