using System.Net;
using System.Text;
using System.Text.Json;
using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;
using RecommendationService.Integrations.Catalog;

namespace RecommendationService.Tests.Integrations.Catalog;

public sealed class CatalogClientTests
{
    [Fact]
    public async Task GetRestaurantsAsync_WhenCatalogReturnsRestaurants_ShouldReturnRestaurants()
    {
        var handler = Handler(JsonResponse(new[]
        {
            new { id = "restaurant-1", name = "Tacos", active = true }
        }));
        var client = CreateClient(handler);

        var restaurants = await client.GetRestaurantsAsync();

        restaurants.Should().ContainSingle()
            .Which.Id.Should().Be("restaurant-1");
        handler.Requests.Should().ContainSingle()
            .Which.RequestUri!.PathAndQuery.Should().Be("/api/customer/restaurants");
    }

    [Fact]
    public async Task GetRestaurantsAsync_WhenCatalogReturnsNull_ShouldReturnEmptyList()
    {
        var client = CreateClient(Handler(TextResponse("null", contentType: "application/json")));

        var restaurants = await client.GetRestaurantsAsync();

        restaurants.Should().BeEmpty();
    }

    [Fact]
    public async Task GetRestaurantsAsync_WhenCatalogReturnsInvalidJson_ShouldThrowJsonException()
    {
        var client = CreateClient(Handler(TextResponse("not-json")));

        var action = () => client.GetRestaurantsAsync();

        await action.Should().ThrowAsync<JsonException>();
    }

    [Fact]
    public async Task GetRestaurantsAsync_WhenCatalogReturnsNoContent_ShouldThrowJsonException()
    {
        var client = CreateClient(Handler(new HttpResponseMessage(HttpStatusCode.OK)));

        var action = () => client.GetRestaurantsAsync();

        await action.Should().ThrowAsync<JsonException>();
    }

    [Fact]
    public async Task GetRestaurantsAsync_WhenHttpClientThrows_ShouldPropagateException()
    {
        var client = CreateClient(Handler(new HttpRequestException("Catalog timeout.")));

        var action = () => client.GetRestaurantsAsync();

        await action.Should().ThrowAsync<HttpRequestException>()
            .WithMessage("Catalog timeout.");
    }

    [Fact]
    public async Task GetRestaurantAsync_WhenCatalogReturnsRestaurant_ShouldReturnRestaurant()
    {
        var handler = Handler(JsonResponse(new { id = "restaurant 1", name = "Tacos" }));
        var client = CreateClient(handler);

        var restaurant = await client.GetRestaurantAsync("restaurant 1");

        restaurant.Should().NotBeNull();
        restaurant!.Id.Should().Be("restaurant 1");
        handler.Requests.Should().ContainSingle()
            .Which.RequestUri!.PathAndQuery.Should().Be("/api/customer/restaurants/restaurant%201");
    }

    [Fact]
    public async Task GetRestaurantAsync_WhenCatalogReturnsNotFound_ShouldReturnNull()
    {
        var client = CreateClient(Handler(new HttpResponseMessage(HttpStatusCode.NotFound)));

        var restaurant = await client.GetRestaurantAsync("missing-restaurant");

        restaurant.Should().BeNull();
    }

    [Fact]
    public async Task GetRestaurantAsync_WhenCatalogReturnsHttpError_ShouldThrowHttpRequestException()
    {
        var client = CreateClient(Handler(new HttpResponseMessage(HttpStatusCode.InternalServerError)));

        var action = () => client.GetRestaurantAsync("restaurant-1");

        var exception = await action.Should().ThrowAsync<HttpRequestException>();
        exception.Which.StatusCode.Should().Be(HttpStatusCode.InternalServerError);
    }

    [Fact]
    public async Task GetBranchesByRestaurantAsync_WhenCatalogReturnsBranches_ShouldReturnBranches()
    {
        var handler = Handler(JsonResponse(new[]
        {
            new { id = "branch-1", name = "Centro", active = true, open = true }
        }));
        var client = CreateClient(handler);

        var branches = await client.GetBranchesByRestaurantAsync("restaurant/1");

        branches.Should().ContainSingle()
            .Which.Id.Should().Be("branch-1");
        handler.Requests.Should().ContainSingle()
            .Which.RequestUri!.PathAndQuery.Should().Be("/api/customer/restaurants/restaurant%2F1/branches");
    }

    [Fact]
    public async Task GetBranchesByRestaurantAsync_WhenCatalogReturnsNull_ShouldReturnEmptyList()
    {
        var client = CreateClient(Handler(TextResponse("null", contentType: "application/json")));

        var branches = await client.GetBranchesByRestaurantAsync("restaurant-1");

        branches.Should().BeEmpty();
    }

    [Fact]
    public async Task GetBranchesByRestaurantAsync_WhenCatalogReturnsHttpError_ShouldReturnEmptyList()
    {
        var client = CreateClient(Handler(new HttpResponseMessage(HttpStatusCode.BadGateway)));

        var branches = await client.GetBranchesByRestaurantAsync("restaurant-1");

        branches.Should().BeEmpty();
    }

    [Fact]
    public async Task GetProductAsync_WhenCatalogReturnsProduct_ShouldReturnProduct()
    {
        var handler = Handler(JsonResponse(new
        {
            id = "product-1",
            restaurantId = "restaurant-1",
            name = "Taco",
            active = true,
            available = true
        }));
        var client = CreateClient(handler);

        var product = await client.GetProductAsync("product/1");

        product.Should().NotBeNull();
        product!.Id.Should().Be("product-1");
        handler.Requests.Should().ContainSingle()
            .Which.RequestUri!.PathAndQuery.Should().Be("/api/customer/products/product%2F1");
    }

    [Fact]
    public async Task GetProductAsync_WhenCatalogReturnsNotFound_ShouldReturnNull()
    {
        var client = CreateClient(Handler(new HttpResponseMessage(HttpStatusCode.NotFound)));

        var product = await client.GetProductAsync("missing-product");

        product.Should().BeNull();
    }

    [Fact]
    public async Task GetProductAsync_WhenCatalogReturnsHttpError_ShouldThrowHttpRequestException()
    {
        var client = CreateClient(Handler(new HttpResponseMessage(HttpStatusCode.ServiceUnavailable)));

        var action = () => client.GetProductAsync("product-1");

        var exception = await action.Should().ThrowAsync<HttpRequestException>();
        exception.Which.StatusCode.Should().Be(HttpStatusCode.ServiceUnavailable);
    }

    private static CatalogClient CreateClient(FakeCatalogHttpMessageHandler handler)
    {
        return new CatalogClient(
            new HttpClient(handler)
            {
                BaseAddress = new Uri("https://catalog.test")
            },
            NullLogger<CatalogClient>.Instance);
    }

    private static FakeCatalogHttpMessageHandler Handler(params object[] responses)
    {
        return new FakeCatalogHttpMessageHandler(responses);
    }

    private static HttpResponseMessage JsonResponse(object value, HttpStatusCode statusCode = HttpStatusCode.OK)
    {
        return TextResponse(JsonSerializer.Serialize(value), statusCode, "application/json");
    }

    private static HttpResponseMessage TextResponse(
        string value,
        HttpStatusCode statusCode = HttpStatusCode.OK,
        string contentType = "text/plain")
    {
        return new HttpResponseMessage(statusCode)
        {
            Content = new StringContent(value, Encoding.UTF8, contentType)
        };
    }

    private sealed class FakeCatalogHttpMessageHandler : HttpMessageHandler
    {
        private readonly Queue<object> _responses;

        public FakeCatalogHttpMessageHandler(IEnumerable<object> responses)
        {
            _responses = new Queue<object>(responses);
        }

        public List<HttpRequestMessage> Requests { get; } = [];

        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request,
            CancellationToken cancellationToken)
        {
            Requests.Add(request);
            var response = _responses.Dequeue();

            return response is Exception exception
                ? Task.FromException<HttpResponseMessage>(exception)
                : Task.FromResult((HttpResponseMessage)response);
        }
    }
}
