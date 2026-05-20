using FluentAssertions;
using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
using OrderService.Exceptions;
using OrderService.Services.Implementations;
using OrderService.Tests.Fakes;
using OrderService.Tests.TestData;

namespace OrderService.Tests.Services;

public sealed class OrderServiceImplTests
{
    [Fact]
    public async Task CreateAsync_ShouldCreateOrder()
    {
        var repository = new InMemoryOrderRepository();
        var service = new OrderServiceImpl(repository);
        var request = OrderTestData.CreateOrderRequest();

        var response = await service.CreateAsync(request);

        response.Id.Should().NotBeNullOrWhiteSpace();
        response.CustomerAccountId.Should().Be(request.CustomerAccountId);
        response.Status.Should().Be(OrderStatus.Created);
        response.Total.Should().Be(100);
    }

    [Fact]
    public async Task GetByIdAsync_WhenOrderExists_ShouldReturnOrder()
    {
        var order = OrderTestData.OrderDocument();
        var service = new OrderServiceImpl(new InMemoryOrderRepository(order));

        var response = await service.GetByIdAsync(order.Id!);

        response.Id.Should().Be(order.Id);
    }

    [Fact]
    public async Task GetByIdAsync_WhenOrderDoesNotExist_ShouldThrowNotFound()
    {
        var service = new OrderServiceImpl(new InMemoryOrderRepository());

        var act = async () => await service.GetByIdAsync("missing-order");

        await act.Should().ThrowAsync<OrderNotFoundException>();
    }

    [Fact]
    public async Task GetByCustomerAccountIdAsync_ShouldReturnCustomerOrders()
    {
        var olderOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-1",
            createdAt: DateTime.UtcNow.AddMinutes(-2));
        var newerOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-1",
            createdAt: DateTime.UtcNow);
        var otherCustomerOrder = OrderTestData.OrderDocument(customerAccountId: "customer-2");
        var service = new OrderServiceImpl(new InMemoryOrderRepository(olderOrder, newerOrder, otherCustomerOrder));

        var responses = await service.GetByCustomerAccountIdAsync("customer-1");

        responses.Should().HaveCount(2);
        responses.Select(order => order.Id).Should().Equal(newerOrder.Id, olderOrder.Id);
    }

    [Fact]
    public async Task GetByRestaurantIdAsync_ShouldReturnRestaurantOrders()
    {
        var restaurantOrder = OrderTestData.OrderDocument(restaurantId: "restaurant-1");
        var otherRestaurantOrder = OrderTestData.OrderDocument(restaurantId: "restaurant-2");
        var service = new OrderServiceImpl(new InMemoryOrderRepository(restaurantOrder, otherRestaurantOrder));

        var responses = await service.GetByRestaurantIdAsync("restaurant-1");

        responses.Should().ContainSingle();
        responses[0].Id.Should().Be(restaurantOrder.Id);
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenTransitionIsValid_ShouldChangeStatus()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        var service = new OrderServiceImpl(new InMemoryOrderRepository(order));

        var response = await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Accepted });

        response.Status.Should().Be(OrderStatus.Accepted);
        response.UpdatedAt.Should().BeAfter(order.CreatedAt);
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenTransitionIsInvalid_ShouldThrow()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        var service = new OrderServiceImpl(new InMemoryOrderRepository(order));

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Ready });

        await act.Should().ThrowAsync<InvalidOrderStatusTransitionException>();
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenOrderIsDelivered_ShouldThrow()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Delivered);
        var service = new OrderServiceImpl(new InMemoryOrderRepository(order));

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Cancelled });

        await act.Should().ThrowAsync<InvalidOrderStatusTransitionException>();
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenOrderIsCancelled_ShouldThrow()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Cancelled);
        var service = new OrderServiceImpl(new InMemoryOrderRepository(order));

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Accepted });

        await act.Should().ThrowAsync<InvalidOrderStatusTransitionException>();
    }
}
