using System.Net;
using System.Net.Http.Json;
using System.Reflection;
using System.Text.Json;
using System.Text.Json.Serialization;
using FluentAssertions;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using MongoDB.Bson;
using MongoDB.Driver;
using OrderService.Controllers;
using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Tests.TestData;

namespace OrderService.Tests.Integration;

public sealed class OrdersControllerIntegrationTests : IClassFixture<OrderApiTestFixture>, IAsyncLifetime
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        Converters = { new JsonStringEnumConverter() }
    };

    private readonly OrderApiTestFixture _fixture;

    public OrdersControllerIntegrationTests(OrderApiTestFixture fixture)
    {
        _fixture = fixture;
    }

    public async Task InitializeAsync()
    {
        await _fixture.Orders.DeleteManyAsync(Builders<OrderService.Domain.Documents.OrderDocument>.Filter.Empty);
        _fixture.Catalog.Reset();
    }

    public Task DisposeAsync()
    {
        return Task.CompletedTask;
    }

    [Fact]
    public async Task PostOrders_ShouldCreateOrderAndPersistItInMongo()
    {
        var request = OrderTestData.CreateOrderRequest();
        _fixture.Catalog.RespondWith(OrderTestData.ValidatedOrderResponse(unitPrice: 50));

        var response = await _fixture.Client.PostAsJsonAsync("/api/orders", request, JsonOptions);
        var body = await response.Content.ReadFromJsonAsync<OrderResponse>(JsonOptions);
        var persistedOrder = await _fixture.Orders
            .Find(order => order.Id == body!.Id)
            .FirstOrDefaultAsync();

        response.StatusCode.Should().Be(HttpStatusCode.Created);
        body.Should().NotBeNull();
        body!.Status.Should().Be(OrderStatus.Created);
        body.Total.Should().Be(100);
        body.PublicTrackingCode.Should().NotBeNullOrWhiteSpace();
        persistedOrder.Should().NotBeNull();
        persistedOrder!.CustomerAccountId.Should().Be(request.CustomerAccountId);
        persistedOrder.PublicTrackingCode.Should().Be(body.PublicTrackingCode);
        _fixture.Catalog.Requests.Should().Be(1);
    }

    [Fact]
    public async Task PostOrders_ShouldPersistValidatedCatalogSnapshot()
    {
        var request = OrderTestData.CreateOrderRequestWithModifier();
        _fixture.Catalog.RespondWith(OrderTestData.ValidatedOrderResponse(
            productName: "Catalog Burger",
            unitPrice: 80,
            modifierPrice: 15));

        var response = await _fixture.Client.PostAsJsonAsync("/api/orders", request, JsonOptions);
        var body = await response.Content.ReadFromJsonAsync<OrderResponse>(JsonOptions);
        var persistedOrder = await _fixture.Orders
            .Find(order => order.Id == body!.Id)
            .FirstOrDefaultAsync();

        response.StatusCode.Should().Be(HttpStatusCode.Created);
        persistedOrder!.Items[0].ProductNameSnapshot.Should().Be("Catalog Burger");
        persistedOrder.Items[0].UnitPriceSnapshot.Should().Be(80);
        persistedOrder.Items[0].SelectedModifiers[0].ModifierOptionName.Should().Be("Catalog Cheese");
        persistedOrder.Total.Should().Be(190);
    }

    [Fact]
    public async Task PostOrders_ShouldIgnoreClientProvidedProductSnapshot()
    {
        var request = OrderTestData.CreateOrderRequest();
        request.Items[0].ProductNameSnapshot = "Client Taco";
        request.Items[0].UnitPriceSnapshot = 1;
        _fixture.Catalog.RespondWith(OrderTestData.ValidatedOrderResponse(
            productName: "Catalog Taco",
            unitPrice: 70));

        var response = await _fixture.Client.PostAsJsonAsync("/api/orders", request, JsonOptions);
        var body = await response.Content.ReadFromJsonAsync<OrderResponse>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.Created);
        body!.Items[0].ProductNameSnapshot.Should().Be("Catalog Taco");
        body.Items[0].UnitPriceSnapshot.Should().Be(70);
        body.Total.Should().Be(140);
    }

    [Fact]
    public async Task PostOrders_WhenCatalogRejectsProduct_ShouldReturnControlledError()
    {
        _fixture.Catalog.RespondWithStatus(StatusCodes.Status400BadRequest);

        var response = await _fixture.Client.PostAsJsonAsync(
            "/api/orders",
            OrderTestData.CreateOrderRequest(),
            JsonOptions);
        var body = await response.Content.ReadFromJsonAsync<ErrorResponse>(JsonOptions);
        var count = await _fixture.Orders.CountDocumentsAsync(
            Builders<OrderService.Domain.Documents.OrderDocument>.Filter.Empty);

        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
        body!.Code.Should().Be("CATALOG_VALIDATION_FAILED");
        count.Should().Be(0);
    }

    [Fact]
    public async Task PostOrders_WhenCatalogServiceIsUnavailable_ShouldReturnControlledError()
    {
        _fixture.Catalog.RespondWithStatus(StatusCodes.Status500InternalServerError);

        var response = await _fixture.Client.PostAsJsonAsync(
            "/api/orders",
            OrderTestData.CreateOrderRequest(),
            JsonOptions);
        var body = await response.Content.ReadFromJsonAsync<ErrorResponse>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.ServiceUnavailable);
        body!.Code.Should().Be("CATALOG_SERVICE_UNAVAILABLE");
    }

    [Fact]
    public async Task GetOrderById_WhenOrderExists_ShouldReturnOrder()
    {
        var order = OrderTestData.OrderDocument();
        await _fixture.Orders.InsertOneAsync(order);

        var response = await _fixture.Client.GetAsync($"/api/orders/{order.Id}");
        var body = await response.Content.ReadFromJsonAsync<OrderResponse>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body!.Id.Should().Be(order.Id);
    }

    [Fact]
    public async Task GetOrderById_WhenOrderDoesNotExist_ShouldReturnNotFound()
    {
        var id = ObjectId.GenerateNewId().ToString();

        var response = await _fixture.Client.GetAsync($"/api/orders/{id}");
        var body = await response.Content.ReadFromJsonAsync<ErrorResponse>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
        body!.Code.Should().Be("ORDER_NOT_FOUND");
    }

    [Fact]
    public async Task GetPublicTracking_WhenOrderExists_ShouldReturnLimitedOrder()
    {
        var order = OrderTestData.OrderDocument(publicTrackingCode: "track-code-1");
        await _fixture.Orders.InsertOneAsync(order);

        var response = await _fixture.Client.GetAsync("/api/orders/public/track/track-code-1");
        var body = await response.Content.ReadFromJsonAsync<PublicOrderTrackingResponse>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body.Should().NotBeNull();
        body!.PublicTrackingCode.Should().Be("track-code-1");
        body.ShortOrderId.Should().Be(order.Id![^8..].ToUpperInvariant());
        body.Status.Should().Be(order.Status);
        body.Items.Should().ContainSingle();
        body.Items[0].ProductNameSnapshot.Should().Be("Taco");
        body.Total.Should().Be(order.Total);
    }

    [Fact]
    public async Task GetPublicTracking_WhenOrderDoesNotExist_ShouldReturnNotFound()
    {
        var response = await _fixture.Client.GetAsync("/api/orders/public/track/missing-code");
        var body = await response.Content.ReadFromJsonAsync<ErrorResponse>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
        body!.Code.Should().Be("ORDER_NOT_FOUND");
    }

    [Fact]
    public void GetPublicTracking_ShouldAllowAnonymousAccess()
    {
        var method = typeof(OrdersController).GetMethod(
            nameof(OrdersController.GetPublicTracking),
            BindingFlags.Instance | BindingFlags.Public);

        method.Should().NotBeNull();
        method!.GetCustomAttribute<AllowAnonymousAttribute>().Should().NotBeNull();
    }

    [Fact]
    public async Task GetOrdersByCustomerAccountId_ShouldReturnOnlyCustomerOrders()
    {
        var customerOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-1",
            createdAt: DateTime.UtcNow.AddMinutes(-1));
        var newestCustomerOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-1",
            createdAt: DateTime.UtcNow);
        var otherOrder = OrderTestData.OrderDocument(customerAccountId: "customer-2");
        await _fixture.Orders.InsertManyAsync([customerOrder, newestCustomerOrder, otherOrder]);

        var response = await _fixture.Client.GetAsync("/api/orders/customer/customer-1");
        var body = await response.Content.ReadFromJsonAsync<List<OrderResponse>>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body.Should().HaveCount(2);
        body!.Select(order => order.Id).Should().Equal(newestCustomerOrder.Id, customerOrder.Id);
    }

    [Fact]
    public async Task GetOrdersByCustomerAccountId_WithStatus_ShouldReturnFilteredResults()
    {
        var createdOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-status",
            status: OrderStatus.Created);
        var acceptedOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-status",
            status: OrderStatus.Accepted);
        await _fixture.Orders.InsertManyAsync([createdOrder, acceptedOrder]);

        var response = await _fixture.Client.GetAsync("/api/orders/customer/customer-status?status=Created");
        var body = await response.Content.ReadFromJsonAsync<List<OrderResponse>>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body.Should().ContainSingle();
        body![0].Id.Should().Be(createdOrder.Id);
        body[0].Status.Should().Be(OrderStatus.Created);
    }

    [Fact]
    public async Task GetOrdersByCustomerAccountId_WithFromAndTo_ShouldFilterByCreatedAt()
    {
        var from = new DateTime(2026, 5, 1, 0, 0, 0, DateTimeKind.Utc);
        var to = new DateTime(2026, 5, 22, 0, 0, 0, DateTimeKind.Utc);
        var olderOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-date-range",
            createdAt: from.AddDays(-1));
        var inRangeOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-date-range",
            createdAt: from.AddDays(10));
        var newerOrder = OrderTestData.OrderDocument(
            customerAccountId: "customer-date-range",
            createdAt: to.AddDays(1));
        await _fixture.Orders.InsertManyAsync([olderOrder, inRangeOrder, newerOrder]);

        var response = await _fixture.Client.GetAsync(
            "/api/orders/customer/customer-date-range?from=2026-05-01&to=2026-05-22");
        var body = await response.Content.ReadFromJsonAsync<List<OrderResponse>>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body.Should().ContainSingle();
        body![0].Id.Should().Be(inRangeOrder.Id);
    }

    [Fact]
    public async Task GetOrdersByCustomerAccountId_WithInvalidDateRange_ShouldReturnBadRequest()
    {
        var response = await _fixture.Client.GetAsync(
            "/api/orders/customer/customer-invalid?from=2026-05-22&to=2026-05-01");
        var body = await response.Content.ReadFromJsonAsync<ErrorResponse>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
        body!.Code.Should().Be("ORDER_VALIDATION_ERROR");
    }

    [Fact]
    public async Task GetOrdersByRestaurantId_ShouldReturnOnlyRestaurantOrders()
    {
        var restaurantOrder = OrderTestData.OrderDocument(restaurantId: "restaurant-1");
        var otherRestaurantOrder = OrderTestData.OrderDocument(restaurantId: "restaurant-2");
        await _fixture.Orders.InsertManyAsync([restaurantOrder, otherRestaurantOrder]);

        var response = await _fixture.Client.GetAsync("/api/orders/restaurant/restaurant-1");
        var body = await response.Content.ReadFromJsonAsync<List<OrderResponse>>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body.Should().ContainSingle();
        body![0].Id.Should().Be(restaurantOrder.Id);
    }

    [Fact]
    public async Task GetOrdersByRestaurantId_WithStatus_ShouldReturnFilteredResults()
    {
        var createdOrder = OrderTestData.OrderDocument(
            restaurantId: "restaurant-status",
            status: OrderStatus.Created);
        var acceptedOrder = OrderTestData.OrderDocument(
            restaurantId: "restaurant-status",
            status: OrderStatus.Accepted);
        await _fixture.Orders.InsertManyAsync([createdOrder, acceptedOrder]);

        var response = await _fixture.Client.GetAsync("/api/orders/restaurant/restaurant-status?status=Accepted");
        var body = await response.Content.ReadFromJsonAsync<List<OrderResponse>>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body.Should().ContainSingle();
        body![0].Id.Should().Be(acceptedOrder.Id);
        body[0].Status.Should().Be(OrderStatus.Accepted);
    }

    [Fact]
    public async Task GetOrdersByBranchId_ShouldReturnOnlyBranchOrders()
    {
        var branchOrder = OrderTestData.OrderDocument(branchId: "branch-1");
        var otherBranchOrder = OrderTestData.OrderDocument(branchId: "branch-2");
        await _fixture.Orders.InsertManyAsync([branchOrder, otherBranchOrder]);

        var response = await _fixture.Client.GetAsync("/api/orders/branch/branch-1");
        var body = await response.Content.ReadFromJsonAsync<List<OrderResponse>>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body.Should().ContainSingle();
        body![0].Id.Should().Be(branchOrder.Id);
        body[0].BranchId.Should().Be("branch-1");
    }

    [Fact]
    public async Task GetOrdersByBranchId_WithStatus_ShouldReturnFilteredResults()
    {
        var createdOrder = OrderTestData.OrderDocument(
            branchId: "branch-status",
            status: OrderStatus.Created);
        var acceptedOrder = OrderTestData.OrderDocument(
            branchId: "branch-status",
            status: OrderStatus.Accepted);
        await _fixture.Orders.InsertManyAsync([createdOrder, acceptedOrder]);

        var response = await _fixture.Client.GetAsync("/api/orders/branch/branch-status?status=Created");
        var body = await response.Content.ReadFromJsonAsync<List<OrderResponse>>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body.Should().ContainSingle();
        body![0].Id.Should().Be(createdOrder.Id);
        body[0].Status.Should().Be(OrderStatus.Created);
    }

    [Fact]
    public async Task PatchOrderStatus_WhenTransitionIsValid_ShouldChangeStatus()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        await _fixture.Orders.InsertOneAsync(order);

        var response = await _fixture.Client.PatchAsJsonAsync(
            $"/api/orders/{order.Id}/status",
            new UpdateOrderStatusRequest { Status = OrderStatus.Accepted },
            JsonOptions);
        var body = await response.Content.ReadFromJsonAsync<OrderResponse>(JsonOptions);
        var persistedOrder = await _fixture.Orders
            .Find(document => document.Id == order.Id)
            .FirstOrDefaultAsync();

        response.StatusCode.Should().Be(HttpStatusCode.OK);
        body!.Status.Should().Be(OrderStatus.Accepted);
        persistedOrder.Status.Should().Be(OrderStatus.Accepted);
    }

    [Fact]
    public async Task PatchOrderStatus_WhenTransitionIsInvalid_ShouldReturnConflict()
    {
        var order = OrderTestData.OrderDocument(status: OrderStatus.Created);
        await _fixture.Orders.InsertOneAsync(order);

        var response = await _fixture.Client.PatchAsJsonAsync(
            $"/api/orders/{order.Id}/status",
            new UpdateOrderStatusRequest { Status = OrderStatus.Ready },
            JsonOptions);
        var body = await response.Content.ReadFromJsonAsync<ErrorResponse>(JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.Conflict);
        body!.Code.Should().Be("INVALID_ORDER_STATUS_TRANSITION");
    }

    [Fact]
    public async Task PostOrders_WhenRequestIsInvalid_ShouldReturnBadRequest()
    {
        var request = OrderTestData.CreateOrderRequest();
        request.Items[0].Quantity = 0;

        var response = await _fixture.Client.PostAsJsonAsync("/api/orders", request, JsonOptions);

        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }
}
