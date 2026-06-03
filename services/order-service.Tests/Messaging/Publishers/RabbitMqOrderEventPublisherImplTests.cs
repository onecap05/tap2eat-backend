using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using OrderService.Config;
using OrderService.Domain.Enums;
using OrderService.Dtos.Responses;
using OrderService.Messaging.Publishers;

namespace OrderService.Tests.Messaging.Publishers;

public sealed class RabbitMqOrderEventPublisherImplTests
{
    [Fact]
    public async Task PublishOrderCreatedAsync_WhenRabbitMqDisabled_ShouldNotThrow()
    {
        var publisher = CreatePublisher(enabled: false);

        var action = async () => await publisher.PublishOrderCreatedAsync(CreateOrder(OrderStatus.Created));

        await action.Should().NotThrowAsync();
    }

    [Fact]
    public async Task PublishOrderStatusChangedAsync_WhenRabbitMqDisabled_ShouldNotThrow()
    {
        var publisher = CreatePublisher(enabled: false);

        var action = async () => await publisher.PublishOrderStatusChangedAsync(
            CreateOrder(OrderStatus.Accepted),
            previousStatus: "Created");

        await action.Should().NotThrowAsync();
    }

    [Fact]
    public async Task PublishOrderCreatedAsync_WhenRabbitMqConnectionFails_ShouldThrow()
    {
        var publisher = CreatePublisher(enabled: true);

        var action = async () => await publisher.PublishOrderCreatedAsync(CreateOrder(OrderStatus.Created));

        await action.Should().ThrowAsync<Exception>();
    }

    [Fact]
    public void CreateOrderStatusChangedEvent_WhenDelivered_ShouldIncludeItems()
    {
        var order = CreateOrder(OrderStatus.Delivered);

        var message = RabbitMqOrderEventPublisherImpl.CreateOrderStatusChangedEvent(
            order,
            previousStatus: "Ready");

        message.NewStatus.Should().Be("Delivered");
        message.Items.Should().ContainSingle();
        message.Items[0].ProductId.Should().Be("product-1");
        message.Items[0].Quantity.Should().Be(2);
        message.Items[0].ProductNameSnapshot.Should().Be("Taco");
    }

    [Fact]
    public void CreateOrderStatusChangedEvent_WhenNotDelivered_ShouldUseEmptyItems()
    {
        var order = CreateOrder(OrderStatus.Preparing);

        var message = RabbitMqOrderEventPublisherImpl.CreateOrderStatusChangedEvent(
            order,
            previousStatus: "Accepted");

        message.NewStatus.Should().Be("Preparing");
        message.Items.Should().BeEmpty();
    }

    [Fact]
    public void CreateOrderStatusChangedEvent_WhenEstimateExists_ShouldIncludeEstimate()
    {
        var estimatedReadyAt = new DateTime(2026, 5, 22, 16, 30, 0, DateTimeKind.Utc);
        var order = CreateOrder(OrderStatus.Accepted, 20, estimatedReadyAt);

        var message = RabbitMqOrderEventPublisherImpl.CreateOrderStatusChangedEvent(
            order,
            previousStatus: "Created");

        message.EstimatedPreparationMinutes.Should().Be(20);
        message.EstimatedReadyAt.Should().Be(estimatedReadyAt);
    }

    [Fact]
    public void CreateOrderStatusChangedEvent_WhenDelivered_ShouldIgnoreItemsWithoutProductId()
    {
        var order = CreateOrder(OrderStatus.Delivered);
        order.Items.Add(new OrderItemResponse
        {
            ProductId = "",
            ProductNameSnapshot = "Ignored",
            Quantity = 1
        });

        var message = RabbitMqOrderEventPublisherImpl.CreateOrderStatusChangedEvent(
            order,
            previousStatus: "Ready");

        message.Items.Should().ContainSingle();
        message.Items[0].ProductId.Should().Be("product-1");
    }

    private static RabbitMqOrderEventPublisherImpl CreatePublisher(bool enabled)
    {
        return new RabbitMqOrderEventPublisherImpl(
            Options.Create(new RabbitMqSettings
            {
                Enabled = enabled,
                HostName = "127.0.0.1",
                Port = 1,
                UserName = "tap2eat",
                Password = "tap2eat",
                ExchangeName = "tap2eat.orders",
                ExchangeType = "topic"
            }),
            NullLogger<RabbitMqOrderEventPublisherImpl>.Instance);
    }

    private static OrderResponse CreateOrder(
        OrderStatus status,
        int? estimatedPreparationMinutes = null,
        DateTime? estimatedReadyAt = null)
    {
        return new OrderResponse
        {
            Id = "order-1",
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            Status = status,
            EstimatedPreparationMinutes = estimatedPreparationMinutes,
            EstimatedReadyAt = estimatedReadyAt,
            Items =
            [
                new OrderItemResponse
                {
                    ProductId = "product-1",
                    ProductNameSnapshot = "Taco",
                    Quantity = 2,
                    UnitPriceSnapshot = 50,
                    Subtotal = 100
                }
            ]
        };
    }
}
