using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;
using RecommendationService.Integrations.Catalog;
using RecommendationService.Messaging.Consumers;
using RecommendationService.Messaging.Events;
using RecommendationService.Repositories;
using RecommendationService.Tests.Fakes;
using System.Text.Json;

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
            Name = "Taco Vegano",
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
            Items = [new OrderStatusChangedItemEvent { ProductId = "product-1", Quantity = 2 }]
        });

        graph.SavedUpdates.Should().ContainSingle();
        graph.SavedUpdates[0].CustomerAccountId.Should().Be("customer-1");
        graph.SavedUpdates[0].RestaurantId.Should().Be("restaurant-1");
        graph.SavedUpdates[0].BranchId.Should().Be("branch-1");
        graph.SavedUpdates[0].Products[0].Quantity.Should().Be(2);
        graph.SavedUpdates[0].Products[0].ProductNameSnapshot.Should().Be("Taco Vegano");
        graph.SavedUpdates[0].Products[0].Tags.Should().Contain(["spicy", "vegan", "allergen:nuts"]);
    }

    [Fact]
    public async Task ProcessRawAsync_WhenDeliveredEvent_ShouldSaveGraphUpdate()
    {
        var graph = new FakeGraphRepository();
        var processor = CreateProcessor(graph: graph);
        var rawMessage = JsonSerializer.Serialize(new OrderStatusChangedEvent
        {
            EventType = "order.status.changed",
            OrderId = "order-1",
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            NewStatus = "Delivered",
            Items =
            [
                new OrderStatusChangedItemEvent
                {
                    ProductId = "product-1",
                    Quantity = 0,
                    ProductNameSnapshot = "Snapshot"
                }
            ]
        });

        await processor.ProcessRawAsync(rawMessage);

        graph.SavedUpdates.Should().ContainSingle();
        graph.SavedUpdates[0].Products.Should().ContainSingle();
        graph.SavedUpdates[0].Products[0].Quantity.Should().Be(1);
        graph.SavedUpdates[0].Products[0].ProductNameSnapshot.Should().Be("Snapshot");
    }

    [Fact]
    public async Task ProcessRawAsync_WhenEventTypeIsMissing_ShouldIgnoreMessage()
    {
        var graph = new FakeGraphRepository();
        var processor = CreateProcessor(graph: graph);

        await processor.ProcessRawAsync("""{"OrderId":"order-1"}""");

        graph.SavedUpdates.Should().BeEmpty();
    }

    [Fact]
    public async Task ProcessRawAsync_WhenEventTypeIsUnsupported_ShouldIgnoreMessage()
    {
        var graph = new FakeGraphRepository();
        var processor = CreateProcessor(graph: graph);

        await processor.ProcessRawAsync("""{"EventType":"order.created","OrderId":"order-1"}""");

        graph.SavedUpdates.Should().BeEmpty();
    }

    [Fact]
    public async Task ProcessRawAsync_WhenJsonIsInvalid_ShouldIgnoreMessage()
    {
        var graph = new FakeGraphRepository();
        var processor = CreateProcessor(graph: graph);

        var action = () => processor.ProcessRawAsync("{ invalid-json");

        await action.Should().NotThrowAsync();
        graph.SavedUpdates.Should().BeEmpty();
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

    [Fact]
    public async Task ProcessAsync_WhenGraphRepositoryFails_ShouldPropagateException()
    {
        var graph = new Mock<IRecommendationGraphRepository>();
        graph
            .Setup(item => item.UpsertDeliveredOrderAsync(
                It.IsAny<DeliveredOrderGraphUpdate>(),
                It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("Neo4j write failed."));
        var processor = new OrderDeliveredEventProcessor(
            new FakeCatalogClient(),
            graph.Object,
            NullLogger<OrderDeliveredEventProcessor>.Instance);

        var action = () => processor.ProcessAsync(new OrderStatusChangedEvent
        {
            OrderId = "order-1",
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            NewStatus = "Delivered"
        });

        await action.Should().ThrowAsync<InvalidOperationException>()
            .WithMessage("Neo4j write failed.");
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
