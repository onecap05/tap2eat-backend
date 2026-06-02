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

        var response = await service.CreateOrderAsync(request);

        catalogClient.Calls.Should().Be(1);
        eventPublisher.OrderCreatedCalls.Should().Be(1);
        response.Id.Should().NotBeNullOrWhiteSpace();
        response.CustomerAccountId.Should().Be(request.CustomerAccountId);
        response.PublicTrackingCode.Should().NotBeNullOrWhiteSpace();
        response.PublicTrackingCode.Should().HaveLength(32);
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

        var response = await service.CreateOrderAsync(request);

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

        var response = await service.CreateOrderAsync(request);

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

        var act = async () => await service.CreateOrderAsync(OrderTestData.CreateOrderRequest());

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

        var act = async () => await service.CreateOrderAsync(OrderTestData.CreateOrderRequest());

        await act.Should().ThrowAsync<CatalogServiceUnavailableException>();
        repository.Orders.Should().BeEmpty();
        eventPublisher.OrderCreatedCalls.Should().Be(0);
    }

    [Fact]
    public async Task GetByIdAsync_WhenOrderExists_ShouldReturnOrder()
    {
        var order = OrderTestData.OrderDocument();
        var service = CreateService(new InMemoryOrderRepository(order));

        var response = await service.GetOrderByIdAsync(order.Id!);

        response.Id.Should().Be(order.Id);
    }

    [Fact]
    public async Task GetByIdAsync_WhenOrderDoesNotExist_ShouldThrowNotFound()
    {
        var service = CreateService(new InMemoryOrderRepository());

        var act = async () => await service.GetOrderByIdAsync("missing-order");

        await act.Should().ThrowAsync<OrderNotFoundException>();
    }

    [Fact]
    public async Task GetPublicTrackingAsync_WhenOrderExists_ShouldReturnLimitedPublicResponse()
    {
        var order = OrderTestData.OrderDocument(publicTrackingCode: "track-code-1");
        var service = CreateService(new InMemoryOrderRepository(order));

        var response = await service.GetOrderPublicTrackingAsync("track-code-1");

        response.PublicTrackingCode.Should().Be("track-code-1");
        response.ShortOrderId.Should().Be(order.Id![^8..].ToUpperInvariant());
        response.Status.Should().Be(order.Status);
        response.Items.Should().ContainSingle();
        response.Items[0].ProductNameSnapshot.Should().Be("Taco");
        response.Total.Should().Be(order.Total);
    }

    [Fact]
    public async Task GetPublicTrackingAsync_WhenOrderDoesNotExist_ShouldThrowNotFound()
    {
        var service = CreateService(new InMemoryOrderRepository());

        var act = async () => await service.GetOrderPublicTrackingAsync("missing-tracking-code");

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

        var responses = await service.GetOrderByCustomerAccountIdAsync("customer-1");

        responses.Should().HaveCount(2);
        responses.Select(order => order.Id).Should().Equal(newerOrder.Id, olderOrder.Id);
    }

    [Fact]
    public async Task GetByCustomerAccountIdAsync_WithStatus_ShouldReturnOnlyMatchingStatus()
    {
        var createdOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-filter",
            status: OrderStatus.Created);
        var acceptedOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-filter",
            status: OrderStatus.Accepted);
        var catalogClient = new FakeCatalogClient(OrderTestData.ValidatedOrderResponse());
        var eventPublisher = new FakeOrderEventPublisher();
        var service = CreateService(
            new InMemoryOrderRepository(createdOrder, acceptedOrder),
            eventPublisher,
            catalogClient);

        var responses = await service.GetOrderByCustomerAccountIdAsync(
            "customer-filter",
            new OrderQueryRequest { Status = OrderStatus.Accepted });

        responses.Should().ContainSingle();
        responses[0].Id.Should().Be(acceptedOrder.Id);
        catalogClient.Calls.Should().Be(0);
        eventPublisher.OrderCreatedCalls.Should().Be(0);
        eventPublisher.OrderStatusChangedCalls.Should().Be(0);
    }

    [Fact]
    public async Task GetByCustomerAccountIdAsync_WithFromAndTo_ShouldReturnOrdersInRange()
    {
        var from = new DateTime(2026, 5, 1, 0, 0, 0, DateTimeKind.Utc);
        var to = new DateTime(2026, 5, 22, 23, 59, 59, DateTimeKind.Utc);
        var olderOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-range",
            createdAt: from.AddDays(-1));
        var inRangeOlderOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-range",
            createdAt: from.AddDays(1));
        var inRangeNewerOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-range",
            createdAt: to.AddDays(-1));
        var newerOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-range",
            createdAt: to.AddDays(1));
        var service = CreateService(new InMemoryOrderRepository(
            olderOrder,
            inRangeOlderOrder,
            inRangeNewerOrder,
            newerOrder));

        var responses = await service.GetOrderByCustomerAccountIdAsync(
            "customer-range",
            new OrderQueryRequest { From = from, To = to });

        responses.Select(order => order.Id).Should().Equal(inRangeNewerOrder.Id, inRangeOlderOrder.Id);
    }

    [Fact]
    public async Task GetByCustomerAccountIdAsync_WithInvalidDateRange_ShouldThrowValidationException()
    {
        var service = CreateService(new InMemoryOrderRepository());

        var act = async () => await service.GetOrderByCustomerAccountIdAsync(
            "customer-invalid",
            InvalidDateRangeQuery());

        await act.Should().ThrowAsync<OrderValidationException>();
    }

    [Fact]
    public async Task GetByRestaurantIdAsync_ShouldReturnRestaurantOrders()
    {
        var restaurantOrder = OrderTestData.OrderDocument(restaurantId: "restaurant-1");
        var otherRestaurantOrder = OrderTestData.OrderDocument(restaurantId: "restaurant-2");
        var service = CreateService(new InMemoryOrderRepository(restaurantOrder, otherRestaurantOrder));

        var responses = await service.GetOrderByRestaurantIdAsync("restaurant-1");

        responses.Should().ContainSingle();
        responses[0].Id.Should().Be(restaurantOrder.Id);
    }

    [Fact]
    public async Task GetByRestaurantIdAsync_WithStatus_ShouldReturnOnlyMatchingStatus()
    {
        var createdOrder = OrderTestData.OrderDocument(
            restaurantId: "restaurant-filter",
            status: OrderStatus.Created);
        var acceptedOrder = OrderTestData.OrderDocument(
            restaurantId: "restaurant-filter",
            status: OrderStatus.Accepted);
        var service = CreateService(new InMemoryOrderRepository(createdOrder, acceptedOrder));

        var responses = await service.GetOrderByRestaurantIdAsync(
            "restaurant-filter",
            new OrderQueryRequest { Status = OrderStatus.Accepted });

        responses.Should().ContainSingle();
        responses[0].Id.Should().Be(acceptedOrder.Id);
    }

    [Fact]
    public async Task GetByRestaurantIdAsync_WithFromAndTo_ShouldReturnOrdersInRange()
    {
        var from = new DateTime(2026, 5, 1, 0, 0, 0, DateTimeKind.Utc);
        var to = new DateTime(2026, 5, 22, 23, 59, 59, DateTimeKind.Utc);
        var olderOrder = OrderTestData.OrderDocument(
            restaurantId: "restaurant-range",
            createdAt: from.AddDays(-1));
        var inRangeOrder = OrderTestData.OrderDocument(
            restaurantId: "restaurant-range",
            createdAt: from.AddDays(3));
        var newerOrder = OrderTestData.OrderDocument(
            restaurantId: "restaurant-range",
            createdAt: to.AddDays(1));
        var service = CreateService(new InMemoryOrderRepository(olderOrder, inRangeOrder, newerOrder));

        var responses = await service.GetOrderByRestaurantIdAsync(
            "restaurant-range",
            new OrderQueryRequest { From = from, To = to });

        responses.Should().ContainSingle();
        responses[0].Id.Should().Be(inRangeOrder.Id);
    }

    [Fact]
    public async Task GetByRestaurantIdAsync_WithInvalidDateRange_ShouldThrowValidationException()
    {
        var service = CreateService(new InMemoryOrderRepository());

        var act = async () => await service.GetOrderByRestaurantIdAsync(
            "restaurant-invalid",
            InvalidDateRangeQuery());

        await act.Should().ThrowAsync<OrderValidationException>();
    }

    [Fact]
    public async Task GetByBranchIdAsync_ShouldReturnOnlyBranchOrders()
    {
        var olderOrder = OrderTestData.OrderDocument(
            branchId: "branch-filter",
            createdAt: DateTime.UtcNow.AddMinutes(-10));
        var newerOrder = OrderTestData.OrderDocument(
            branchId: "branch-filter",
            createdAt: DateTime.UtcNow);
        var otherBranchOrder = OrderTestData.OrderDocument(branchId: "other-branch");
        var service = CreateService(new InMemoryOrderRepository(olderOrder, newerOrder, otherBranchOrder));

        var responses = await service.GetOrderByBranchIdAsync(
            "branch-filter",
            new OrderQueryRequest());

        responses.Should().HaveCount(2);
        responses.Select(order => order.Id).Should().Equal(newerOrder.Id, olderOrder.Id);
    }

    [Fact]
    public async Task GetByBranchIdAsync_WithStatus_ShouldReturnOnlyMatchingStatus()
    {
        var createdOrder = OrderTestData.OrderDocument(
            branchId: "branch-status",
            status: OrderStatus.Created);
        var acceptedOrder = OrderTestData.OrderDocument(
            branchId: "branch-status",
            status: OrderStatus.Accepted);
        var service = CreateService(new InMemoryOrderRepository(createdOrder, acceptedOrder));

        var responses = await service.GetOrderByBranchIdAsync(
            "branch-status",
            new OrderQueryRequest { Status = OrderStatus.Accepted });

        responses.Should().ContainSingle();
        responses[0].Id.Should().Be(acceptedOrder.Id);
    }

    [Fact]
    public async Task GetByBranchIdAsync_WithFromAndTo_ShouldReturnOrdersInRange()
    {
        var from = new DateTime(2026, 5, 1, 0, 0, 0, DateTimeKind.Utc);
        var to = new DateTime(2026, 5, 22, 23, 59, 59, DateTimeKind.Utc);
        var olderOrder = OrderTestData.OrderDocument(
            branchId: "branch-range",
            createdAt: from.AddDays(-1));
        var inRangeOrder = OrderTestData.OrderDocument(
            branchId: "branch-range",
            createdAt: from.AddDays(5));
        var newerOrder = OrderTestData.OrderDocument(
            branchId: "branch-range",
            createdAt: to.AddDays(1));
        var service = CreateService(new InMemoryOrderRepository(olderOrder, inRangeOrder, newerOrder));

        var responses = await service.GetOrderByBranchIdAsync(
            "branch-range",
            new OrderQueryRequest { From = from, To = to });

        responses.Should().ContainSingle();
        responses[0].Id.Should().Be(inRangeOrder.Id);
    }

    [Fact]
    public async Task GetByBranchIdAsync_WithInvalidDateRange_ShouldThrowValidationException()
    {
        var service = CreateService(new InMemoryOrderRepository());

        var act = async () => await service.GetOrderByBranchIdAsync(
            "branch-invalid",
            InvalidDateRangeQuery());

        await act.Should().ThrowAsync<OrderValidationException>();
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
    public async Task UpdateStatusAsync_WhenAcceptedWithEstimatedPreparationTime_ShouldCalculateEstimatedReadyAt()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        var service = CreateService(new InMemoryOrderRepository(order), new FakeOrderEventPublisher());

        var response = await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest
            {
                Status = OrderStatus.Accepted,
                EstimatedPreparationMinutes = 20
            });

        response.Status.Should().Be(OrderStatus.Accepted);
        response.EstimatedPreparationMinutes.Should().Be(20);
        response.EstimatedReadyAt.Should().NotBeNull();
        response.EstimatedReadyAt.Should().BeCloseTo(response.UpdatedAt.AddMinutes(20), TimeSpan.FromSeconds(2));
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenAcceptedWithoutEstimatedPreparationTime_ShouldKeepEstimateEmpty()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        var service = CreateService(new InMemoryOrderRepository(order));

        var response = await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Accepted });

        response.Status.Should().Be(OrderStatus.Accepted);
        response.EstimatedPreparationMinutes.Should().BeNull();
        response.EstimatedReadyAt.Should().BeNull();
    }

    [Theory]
    [InlineData(-1)]
    [InlineData(0)]
    public async Task UpdateStatusAsync_WhenEstimatedPreparationTimeIsNotPositive_ShouldThrow(int minutes)
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        var eventPublisher = new FakeOrderEventPublisher();
        var service = CreateService(new InMemoryOrderRepository(order), eventPublisher);

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest
            {
                Status = OrderStatus.Accepted,
                EstimatedPreparationMinutes = minutes
            });

        await act.Should().ThrowAsync<OrderValidationException>();
        eventPublisher.OrderStatusChangedCalls.Should().Be(0);
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenEstimatedPreparationTimeIsTooHigh_ShouldThrow()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        var eventPublisher = new FakeOrderEventPublisher();
        var service = CreateService(new InMemoryOrderRepository(order), eventPublisher);

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest
            {
                Status = OrderStatus.Accepted,
                EstimatedPreparationMinutes = 241
            });

        await act.Should().ThrowAsync<OrderValidationException>();
        eventPublisher.OrderStatusChangedCalls.Should().Be(0);
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenOtherStatusReceivesEstimatedPreparationTime_ShouldThrow()
    {
        var order = OrderTestData.OrderDocument(
            status: OrderStatus.Accepted,
            estimatedPreparationMinutes: 15,
            estimatedReadyAt: DateTime.UtcNow.AddMinutes(15));
        var eventPublisher = new FakeOrderEventPublisher();
        var service = CreateService(new InMemoryOrderRepository(order), eventPublisher);

        var act = async () => await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest
            {
                Status = OrderStatus.Preparing,
                EstimatedPreparationMinutes = 10
            });

        await act.Should().ThrowAsync<OrderValidationException>();
        eventPublisher.OrderStatusChangedCalls.Should().Be(0);
    }

    [Fact]
    public async Task UpdateStatusAsync_WhenOtherStatusDoesNotReceiveEstimatedPreparationTime_ShouldKeepExistingEstimate()
    {
        var estimatedReadyAt = DateTime.UtcNow.AddMinutes(15);
        var order = OrderTestData.OrderDocument(
            status: OrderStatus.Accepted,
            estimatedPreparationMinutes: 15,
            estimatedReadyAt: estimatedReadyAt);
        var service = CreateService(new InMemoryOrderRepository(order));

        var response = await service.UpdateStatusAsync(
            order.Id!,
            new UpdateOrderStatusRequest { Status = OrderStatus.Preparing });

        response.Status.Should().Be(OrderStatus.Preparing);
        response.EstimatedPreparationMinutes.Should().Be(15);
        response.EstimatedReadyAt.Should().Be(estimatedReadyAt);
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
        return CreateService(
            repository,
            eventPublisher,
            new FakeCatalogClient(OrderTestData.ValidatedOrderResponse()));
    }

    private static OrderServiceImpl CreateService(
        InMemoryOrderRepository repository,
        FakeOrderEventPublisher eventPublisher,
        FakeCatalogClient catalogClient)
    {
        return new OrderServiceImpl(
            repository,
            catalogClient,
            eventPublisher);
    }

    private static OrderQueryRequest InvalidDateRangeQuery()
    {
        return new OrderQueryRequest
        {
            From = new DateTime(2026, 5, 22, 0, 0, 0, DateTimeKind.Utc),
            To = new DateTime(2026, 5, 1, 0, 0, 0, DateTimeKind.Utc)
        };
    }
}
