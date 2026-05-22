using System.Text.Json;
using FinanceService.Messaging.Events;
using FinanceService.Services.Interfaces;

namespace FinanceService.Messaging.Consumers;

public sealed class OrderEventProcessorImpl : IOrderEventProcessor
{
    private const string EventTypeProperty = "EventType";
    private const string OrderCreatedEventType = "order.created";
    private const string OrderStatusChangedEventType = "order.status.changed";

    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true
    };

    private readonly IPaymentService _paymentService;
    private readonly ILogger<OrderEventProcessorImpl> _logger;

    public OrderEventProcessorImpl(
        IPaymentService paymentService,
        ILogger<OrderEventProcessorImpl> logger)
    {
        _paymentService = paymentService;
        _logger = logger;
    }

    public async Task ProcessAsync(
        string rawMessage,
        CancellationToken cancellationToken = default)
    {
        try
        {
            using var document = JsonDocument.Parse(rawMessage);

            if (!document.RootElement.TryGetProperty(EventTypeProperty, out var eventTypeElement))
            {
                _logger.LogWarning("Ignoring order event without EventType.");
                return;
            }

            var eventType = eventTypeElement.GetString();

            if (string.Equals(eventType, OrderCreatedEventType, StringComparison.OrdinalIgnoreCase))
            {
                var orderCreatedEvent = JsonSerializer.Deserialize<OrderCreatedEvent>(
                    rawMessage,
                    JsonOptions);

                if (orderCreatedEvent is null)
                {
                    _logger.LogWarning("Ignoring empty order.created payload.");
                    return;
                }

                await _paymentService.CreatePendingPaymentFromOrderAsync(
                    orderCreatedEvent,
                    cancellationToken);

                return;
            }

            if (string.Equals(eventType, OrderStatusChangedEventType, StringComparison.OrdinalIgnoreCase))
            {
                var orderStatusChangedEvent = JsonSerializer.Deserialize<OrderStatusChangedEvent>(
                    rawMessage,
                    JsonOptions);

                if (orderStatusChangedEvent is null)
                {
                    _logger.LogWarning("Ignoring empty order.status.changed payload.");
                    return;
                }

                await _paymentService.HandleOrderStatusChangedAsync(
                    orderStatusChangedEvent,
                    cancellationToken);

                return;
            }

            _logger.LogWarning("Ignoring unknown order event type: {EventType}", eventType);
        }
        catch (JsonException exception)
        {
            _logger.LogError(
                "Ignoring invalid order event JSON payload: {Message}",
                exception.Message);
        }
    }
}
