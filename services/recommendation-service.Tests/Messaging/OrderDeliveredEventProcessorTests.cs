using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;
using RecommendationService.Integrations.Catalog;
using RecommendationService.Messaging.Consumers;
using RecommendationService.Messaging.Events;
using RecommendationService.Tests.Fakes;

namespace RecommendationService.Tests.Messaging;

public sealed class OrderDeliveredEventProcessorTests
{
    [Fact]
    public async Task ProcessAsync_WhenStatusIsNotDelivered_ShouldIgnoreEvent()
    {
        var graph = new FakeGraphRepository();
        var processor = CreateProcessor(graph: graph);

        await processor.ProcessAsync(new OrderStatusChangedEvent
        {
            OrderId = "order-1",
            NewStatus = "Preparing"
        });

        graph.SavedUpdates.Should().BeEmpty();
    }

    [Fact]
    public async Task ProcessAsync_WhenDelivered_ShouldSaveGraphUpdate()
    {
        var catalogClient = new FakeCatalogClient();
        var graph = new FakeGraphRepository();

        catalogClient.ProductsById["product-1"] = new CatalogProductResponse
        {
            Id = "product-1",
            Tags = ["spicy"],
            DietaryFlags = ["vegan"],
            Allergens = ["nuts"]
        };

        var processor = CreateProcessor(catalogClient, graph);

        await processor.ProcessAsync(new OrderStatusChangedEvent
        {
            OrderId = "order-1",
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            NewStatus = "Delivered",
            OccurredAt = DateTime.UtcNow,
            Items = [new OrderStatusChangedItemEvent { ProductId = "product-1", Quantity = 1 }]
        });

        graph.SavedUpdates.Should().ContainSingle();
        graph.SavedUpdates[0].CustomerAccountId.Should().Be("customer-1");
        graph.SavedUpdates[0].Products[0].Tags.Should().Contain(["spicy", "vegan", "allergen:nuts"]);
    }

    [Fact]
    public async Task ProcessAsync_WhenDeliveredWithoutProductIds_ShouldSaveOrderGraphWithoutThrowing()
    {
        var graph = new FakeGraphRepository();
        var processor = CreateProcessor(graph: graph);

        await processor.ProcessAsync(new OrderStatusChangedEvent
        {
            OrderId = "order-1",
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            NewStatus = "Delivered",
            Items =
            [
                new OrderStatusChangedItemEvent { ProductId = "", Quantity = 1 }
            ]
        });

        graph.SavedUpdates.Should().ContainSingle();
        graph.SavedUpdates[0].Products.Should().BeEmpty();
    }

    private static OrderDeliveredEventProcessor CreateProcessor(
        FakeCatalogClient? catalogClient = null,
        FakeGraphRepository? graph = null)
    {
        return new OrderDeliveredEventProcessor(
            catalogClient ?? new FakeCatalogClient(),
            graph ?? new FakeGraphRepository(),
            NullLogger<OrderDeliveredEventProcessor>.Instance);
    }
}
