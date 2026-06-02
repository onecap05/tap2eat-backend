using FluentAssertions;
using OrderService.Domain.Enums;
using OrderService.Dtos.Responses;
using OrderService.Messaging.Publishers;

namespace OrderService.Tests.Messaging.Publishers;

public sealed class RabbitMqOrderEventPublisherImplTests
{
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
