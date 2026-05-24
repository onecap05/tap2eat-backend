using System.Text.Json;
using RecommendationService.Integrations.Catalog;
using RecommendationService.Messaging.Events;
using RecommendationService.Repositories;

namespace RecommendationService.Messaging.Consumers;

public sealed class OrderDeliveredEventProcessor : IOrderDeliveredEventProcessor
{
    private const string DeliveredStatus = "Delivered";
    private const string OrderStatusChangedEventType = "order.status.changed";
    private const string EventTypeProperty = "EventType";

    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true
    };

    private readonly ICatalogClient _catalogClient;
    private readonly IRecommendationGraphRepository _graphRepository;
    private readonly ILogger<OrderDeliveredEventProcessor> _logger;

    public OrderDeliveredEventProcessor(
        ICatalogClient catalogClient,
        IRecommendationGraphRepository graphRepository,
        ILogger<OrderDeliveredEventProcessor> logger)
    {
        _catalogClient = catalogClient;
        _graphRepository = graphRepository;
        _logger = logger;
    }

    public async Task ProcessRawAsync(
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
            if (!string.Equals(eventType, OrderStatusChangedEventType, StringComparison.OrdinalIgnoreCase))
            {
                _logger.LogWarning("Ignoring unsupported order event type for recommendations: {EventType}", eventType);
                return;
            }

            var statusChangedEvent = JsonSerializer.Deserialize<OrderStatusChangedEvent>(rawMessage, JsonOptions);
            if (statusChangedEvent is null)
            {
                _logger.LogWarning("Ignoring empty order.status.changed payload.");
                return;
            }

            await ProcessAsync(statusChangedEvent, cancellationToken);
        }
        catch (JsonException exception)
        {
            _logger.LogError("Ignoring invalid recommendation order event JSON payload: {Message}", exception.Message);
        }
    }

    public async Task ProcessAsync(
        OrderStatusChangedEvent orderStatusChangedEvent,
        CancellationToken cancellationToken = default)
    {
        if (!string.Equals(orderStatusChangedEvent.NewStatus, DeliveredStatus, StringComparison.OrdinalIgnoreCase))
        {
            _logger.LogInformation(
                "Ignoring order status event for recommendation graph. OrderId={OrderId}, NewStatus={NewStatus}",
                orderStatusChangedEvent.OrderId,
                orderStatusChangedEvent.NewStatus);
            return;
        }

        var graphProducts = new List<DeliveredProductGraphUpdate>();

        foreach (var item in orderStatusChangedEvent.Items.Where(item => !string.IsNullOrWhiteSpace(item.ProductId)))
        {
            var product = await _catalogClient.GetProductAsync(item.ProductId, cancellationToken);
            var tags = BuildTags(product);

            graphProducts.Add(new DeliveredProductGraphUpdate
            {
                ProductId = item.ProductId,
                Tags = tags
            });
        }

        await _graphRepository.UpsertDeliveredOrderAsync(
            new DeliveredOrderGraphUpdate
            {
                CustomerAccountId = orderStatusChangedEvent.CustomerAccountId,
                RestaurantId = orderStatusChangedEvent.RestaurantId,
                BranchId = orderStatusChangedEvent.BranchId,
                DeliveredAt = orderStatusChangedEvent.OccurredAt,
                Products = graphProducts
            },
            cancellationToken);
    }

    private static List<string> BuildTags(CatalogProductResponse? product)
    {
        if (product is null)
        {
            return [];
        }

        return product.Tags
            .Concat(product.DietaryFlags)
            .Concat(product.Allergens.Select(allergen => $"allergen:{allergen}"))
            .Where(tag => !string.IsNullOrWhiteSpace(tag))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();
    }
}
