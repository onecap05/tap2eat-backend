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
        var catalogClient = new FakeCatalogClient(OrderTestData.ValidatedOrderResponse(unitPrice: 50));
        var eventPublisher = new FakeOrderEventPublisher();
        var service = new OrderServiceImpl(repository, catalogClient, eventPublisher);
        var request = OrderTestData.CreateOrderRequest();

        var response = await service.CreateAsync(request);

        catalogClient.Calls.Should().Be(1);
        eventPublisher.OrderCreatedCalls.Should().Be(1);
        response.Id.Should().NotBeNullOrWhiteSpace();
        response.CustomerAccountId.Should().Be(request.CustomerAccountId);
        response.Status.Should().Be(OrderStatus.Created);
        response.Total.Should().Be(100);
    }

    [Fact]
    public async Task CreateAsync_ShouldUseCatalogPriceInsteadOfClientPrice()
    {
        var repository = new InMemoryOrderRepository();
        var eventPublisher = new FakeOrderEventPublisher();
        var service = new OrderServiceImpl(
            repository,
            new FakeCatalogClient(OrderTestData.ValidatedOrderResponse(unitPrice: 75)),
            eventPublisher);

        var request = OrderTestData.CreateOrderRequest();
        request.Items[0].UnitPriceSnapshot = 1;

        var response = await service.CreateAsync(request);

        eventPublisher.OrderCreatedCalls.Should().Be(1);
        response.Items[0].UnitPriceSnapshot.Should().Be(75);
        response.Total.Should().Be(150);
        repository.Orders.Should().ContainSingle(order => order.Total == 150);
    }

    [Fact]
    public async Task CreateAsync_ShouldUseCatalogProductNameAndModifiers()
    {
        var repository = new InMemoryOrderRepository();
        var eventPublisher = new FakeOrderEventPublisher();
        var service = new OrderServiceImpl(
            repository,
            new FakeCatalogClient(OrderTestData.ValidatedOrderResponse(
                productName: "Catalog Burger",
                unitPrice: 80,
                modifierPrice: 15)),
            eventPublisher);

        var request = OrderTestData.CreateOrderRequestWithModifier();
        request.Items[0].ProductNameSnapshot = "Client Burger";
        request.Items[0].SelectedModifiers[0].ModifierOptionName = "Client Cheese";

        var response = await service.CreateAsync(request);

        eventPublisher.OrderCreatedCalls.Should().Be(1);
        response.Items[0].ProductNameSnapshot.Should().Be("Catalog Burger");
        response.Items[0].SelectedModifiers.Should().ContainSingle();
        response.Items[0].SelectedModifiers[0].ModifierOptionName.Should().Be("Catalog Cheese");
        response.Total.Should().Be(190);
    }

    [Fact]
    public async Task CreateAsync_WhenCatalogRejectsOrder_ShouldNotSaveOrder()
    {
        var repository = new InMemoryOrderRepository();
        var eventPublisher = new FakeOrderEventPublisher();
        var service = new OrderServiceImpl(
            repository,
            new FakeCatalogClient((_, _) => throw new CatalogValidationException("Invalid product.")),
            eventPublisher);

        var act = async () => await service.CreateAsync(OrderTestData.CreateOrderRequest());

        await act.Should().ThrowAsync<CatalogValidationException>();
        repository.Orders.Should().BeEmpty();
        eventPublisher.OrderCreatedCalls.Should().Be(0);
    }

    [Fact]
    public async Task CreateAsync_WhenCatalogIsUnavailable_ShouldThrowControlledError()
    {
        var repository = new InMemoryOrderRepository();
        var eventPublisher = new FakeOrderEventPublisher();
        var service = new OrderServiceImpl(
            repository,
            new FakeCatalogClient((_, _) => throw new CatalogServiceUnavailableException()),
            eventPublisher);

        var act = async () => await service.CreateAsync(OrderTestData.CreateOrderRequest());

        await act.Should().ThrowAsync<CatalogServiceUnavailableException>();
        repository.Orders.Should().BeEmpty();
        eventPublisher.OrderCreatedCalls.Should().Be(0);
    }

    [Fact]
    public async Task GetByIdAsync_WhenOrderExists_ShouldReturnOrder()
    {
        var order = OrderTestData.OrderDocument();
        var service = CreateService(new InMemoryOrderRepository(order));

        var response = await service.GetByIdAsync(order.Id!);

        response.Id.Should().Be(order.Id);
    }

    [Fact]
    public async Task GetByIdAsync_WhenOrderDoesNotExist_ShouldThrowNotFound()
    {
        var service = CreateService(new InMemoryOrderRepository());

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
        var service = CreateService(new InMemoryOrderRepository(olderOrder, newerOrder, otherCustomerOrder));

        var responses = await service.GetByCustomerAccountIdAsync("customer-1");

        responses.Should().HaveCount(2);
        responses.Select(order => order.Id).Should().Equal(newerOrder.Id, olderOrder.Id);
    }

    [Fact]
    public async Task GetByRestaurantIdAsync_ShouldReturnRestaurantOrders()
    {
        var restaurantOrder = OrderTestData.OrderDocument(restaurantId: "restaurant-1");
        var otherRestaurantOrder = OrderTestData.OrderDocument(restaurantId: "restaurant-2");
        var service = CreateService(new InMemoryOrderRepository(restaurantOrder, otherRestaurantOrder));

        var responses = await service.GetByRestaurantIdAsync("restaurant-1");

        responses.Should().ContainSingle();
        responses[0].Id.Should().Be(restaurantOrder.Id);
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenTransitionIsValid_ShouldChangeStatus()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        var repository = new InMemoryOrderRepository(order);
        var eventPublisher = new FakeOrderEventPublisher();
        var service = CreateService(repository, eventPublisher);

        var response = await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Accepted });

        response.Status.Should().Be(OrderStatus.Accepted);
        response.UpdatedAt.Should().BeAfter(order.CreatedAt);
        eventPublisher.OrderStatusChangedCalls.Should().Be(1);
        eventPublisher.LastPreviousStatus.Should().Be(OrderStatus.Created.ToString());
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenTransitionIsInvalid_ShouldThrow()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        var repository = new InMemoryOrderRepository(order);
        var eventPublisher = new FakeOrderEventPublisher();
        var service = CreateService(repository, eventPublisher);

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Ready });

        await act.Should().ThrowAsync<InvalidOrderStatusTransitionException>();
        eventPublisher.OrderStatusChangedCalls.Should().Be(0);
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenOrderIsDelivered_ShouldThrow()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Delivered);
        var repository = new InMemoryOrderRepository(order);
        var eventPublisher = new FakeOrderEventPublisher();
        var service = CreateService(repository, eventPublisher);

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Cancelled });

        await act.Should().ThrowAsync<InvalidOrderStatusTransitionException>();
        eventPublisher.OrderStatusChangedCalls.Should().Be(0);
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenOrderIsCancelled_ShouldThrow()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Cancelled);
        var repository = new InMemoryOrderRepository(order);
        var eventPublisher = new FakeOrderEventPublisher();
        var service = CreateService(repository, eventPublisher);

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Accepted });

        await act.Should().ThrowAsync<InvalidOrderStatusTransitionException>();
        eventPublisher.OrderStatusChangedCalls.Should().Be(0);
    }

    private static OrderServiceImpl CreateService(InMemoryOrderRepository repository)
    {
        return CreateService(repository, new FakeOrderEventPublisher());
    }

    private static OrderServiceImpl CreateService(
        InMemoryOrderRepository repository,
        FakeOrderEventPublisher eventPublisher)
    {
        return new OrderServiceImpl(
            repository,
            new FakeCatalogClient(OrderTestData.ValidatedOrderResponse()),
            eventPublisher);
    }
}