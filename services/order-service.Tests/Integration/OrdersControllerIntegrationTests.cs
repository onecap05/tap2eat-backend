using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using FluentAssertions;
using MongoDB.Bson;
using MongoDB.Driver;
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
    }

    public Task DisposeAsync()
    {
        return Task.CompletedTask;
    }

    [Fact]
    public async Task PostOrders_ShouldCreateOrderAndPersistItInMongo()
    {
        var request = OrderTestData.CreateOrderRequest();

        var response = await _fixture.Client.PostAsJsonAsync("/api/orders", request, JsonOptions);
        var body = await response.Content.ReadFromJsonAsync<OrderResponse>(JsonOptions);
        var persistedOrder = await _fixture.Orders
            .Find(order => order.Id == body!.Id)
            .FirstOrDefaultAsync();

        response.StatusCode.Should().Be(HttpStatusCode.Created);
        body.Should().NotBeNull();
        body!.Status.Should().Be(OrderStatus.Created);
        body.Total.Should().Be(100);
        persistedOrder.Should().NotBeNull();
        persistedOrder!.CustomerAccountId.Should().Be(request.CustomerAccountId);
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
