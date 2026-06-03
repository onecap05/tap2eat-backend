using FluentAssertions;
using Microsoft.Extensions.Options;
using MongoDB.Driver;
using Moq;
using OrderService.Config;
using OrderService.Domain.Documents;
using OrderService.Domain.Enums;
using OrderService.Repositories.Implementations;

namespace OrderService.Tests.Repositories;

public sealed class OrderRepositoryTests
{
    [Fact]
    public async Task FindByIdAsync_WhenIdIsInvalid_ShouldReturnNullWithoutQueryingCollection()
    {
        var collection = new Mock<IMongoCollection<OrderDocument>>();
        var repository = CreateRepository(collection);

        var order = await repository.FindByIdAsync("not-object-id");

        order.Should().BeNull();
        collection.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task FindByPublicTrackingCodeAsync_WhenCodeIsEmpty_ShouldReturnNullWithoutQueryingCollection()
    {
        var collection = new Mock<IMongoCollection<OrderDocument>>();
        var repository = CreateRepository(collection);

        var order = await repository.FindByPublicTrackingCodeAsync(" ");

        order.Should().BeNull();
        collection.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenIdIsInvalid_ShouldReturnNullWithoutQueryingCollection()
    {
        var collection = new Mock<IMongoCollection<OrderDocument>>();
        var repository = CreateRepository(collection);

        var order = await repository.UpdateStatusAsync(
            "not-object-id",
            OrderStatus.Accepted,
            estimatedPreparationMinutes: null,
            estimatedReadyAt: null,
            DateTime.UtcNow);

        order.Should().BeNull();
        collection.VerifyNoOtherCalls();
    }

    private static OrderRepository CreateRepository(Mock<IMongoCollection<OrderDocument>> collection)
    {
        var database = new Mock<IMongoDatabase>();
        database
            .Setup(item => item.GetCollection<OrderDocument>(
                "orders",
                null))
            .Returns(collection.Object);

        var mongoClient = new Mock<IMongoClient>();
        mongoClient
            .Setup(item => item.GetDatabase(
                "tap2eat_order_test",
                null))
            .Returns(database.Object);

        return new OrderRepository(
            mongoClient.Object,
            Options.Create(new MongoDbSettings
            {
                DatabaseName = "tap2eat_order_test",
                OrdersCollectionName = "orders"
            }));
    }
}
