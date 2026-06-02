using FluentAssertions;
using RecommendationService.Integrations.Catalog;
using RecommendationService.Repositories;
using RecommendationService.Services;
using RecommendationService.Tests.Fakes;

namespace RecommendationService.Tests.Services;

public sealed class FavoriteServiceImplTests
{
    [Fact]
    public async Task AddRestaurantAsync_ShouldMarkRestaurantAsFavorite()
    {
        var (service, catalog, graph) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));

        var response = await service.AddRestaurantAsync("customer-1", "restaurant-1");

        response.RestaurantId.Should().Be("restaurant-1");
        response.RestaurantName.Should().Be("Restaurant restaurant-1");
        graph.FavoriteRestaurants.Should().ContainKey(("customer-1", "restaurant-1"));
    }

    [Fact]
    public async Task RemoveRestaurantAsync_ShouldRemoveRestaurantFavorite()
    {
        var (service, catalog, graph) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));
        await service.AddRestaurantAsync("customer-1", "restaurant-1");

        await service.RemoveRestaurantAsync("customer-1", "restaurant-1");

        graph.FavoriteRestaurants.Should().NotContainKey(("customer-1", "restaurant-1"));
    }

    [Fact]
    public async Task AddRestaurantAsync_ShouldNotDuplicateRestaurantFavorite()
    {
        var (service, catalog, graph) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));

        await service.AddRestaurantAsync("customer-1", "restaurant-1");
        await service.AddRestaurantAsync("customer-1", "restaurant-1");

        graph.FavoriteRestaurants.Should().ContainSingle();
    }

    [Fact]
    public async Task AddProductAsync_ShouldMarkBaseProductAsFavorite()
    {
        var (service, catalog, graph) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));
        catalog.ProductsById["product-1"] = Product("product-1", "restaurant-1");

        var response = await service.AddProductAsync("customer-1", "product-1");

        response.ProductId.Should().Be("product-1");
        response.RestaurantId.Should().Be("restaurant-1");
        response.Price.Should().Be(99);
        graph.FavoriteProducts.Should().ContainKey(("customer-1", "product-1"));
    }

    [Fact]
    public async Task RemoveProductAsync_ShouldRemoveProductFavorite()
    {
        var (service, catalog, graph) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));
        catalog.ProductsById["product-1"] = Product("product-1", "restaurant-1");
        await service.AddProductAsync("customer-1", "product-1");

        await service.RemoveProductAsync("customer-1", "product-1");

        graph.FavoriteProducts.Should().NotContainKey(("customer-1", "product-1"));
    }

    [Fact]
    public async Task AddProductAsync_ShouldNotDuplicateProductFavorite()
    {
        var (service, catalog, graph) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));
        catalog.ProductsById["product-1"] = Product("product-1", "restaurant-1");

        await service.AddProductAsync("customer-1", "product-1");
        await service.AddProductAsync("customer-1", "product-1");

        graph.FavoriteProducts.Should().ContainSingle();
    }

    [Fact]
    public async Task GetCustomerFavoritesAsync_ShouldReturnRestaurantsAndProducts()
    {
        var (service, catalog, _) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));
        catalog.ProductsById["product-1"] = Product("product-1", "restaurant-1");
        await service.AddRestaurantAsync("customer-1", "restaurant-1");
        await service.AddProductAsync("customer-1", "product-1");

        var response = await service.GetCustomerFavoritesAsync("customer-1");

        response.Restaurants.Should().ContainSingle(item => item.RestaurantId == "restaurant-1");
        response.Products.Should().ContainSingle(item => item.ProductId == "product-1");
    }

    [Fact]
    public async Task GetCustomerFavoriteStatusAsync_ShouldReturnRequestedFavoriteIds()
    {
        var (service, catalog, _) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));
        catalog.ProductsById["product-1"] = Product("product-1", "restaurant-1");
        await service.AddRestaurantAsync("customer-1", "restaurant-1");
        await service.AddProductAsync("customer-1", "product-1");

        var response = await service.GetCustomerFavoriteStatusAsync(
            "customer-1",
            ["restaurant-1", "restaurant-2"],
            ["product-1", "product-2"]);

        response.RestaurantIds.Should().BeEquivalentTo(["restaurant-1"]);
        response.ProductIds.Should().BeEquivalentTo(["product-1"]);
    }

    [Fact]
    public async Task GetFeaturedProductAsync_ShouldChooseProductWithMostFavorites()
    {
        var (service, catalog, graph) = CreateService();
        catalog.ProductsById["product-1"] = Product("product-1", "restaurant-1");
        catalog.ProductsById["product-2"] = Product("product-2", "restaurant-1");
        graph.FeaturedProductCandidates.Add(new FeaturedProductGraphRecord("product-2", 3));
        graph.FeaturedProductCandidates.Add(new FeaturedProductGraphRecord("product-1", 1));

        var response = await service.GetFeaturedProductAsync("restaurant-1");

        response.Should().NotBeNull();
        response!.ProductId.Should().Be("product-2");
        response.FavoriteCount.Should().Be(3);
    }

    [Fact]
    public async Task GetFeaturedProductAsync_WithoutFavorites_ShouldReturnNull()
    {
        var (service, _, _) = CreateService();

        var response = await service.GetFeaturedProductAsync("restaurant-1");

        response.Should().BeNull();
    }

    [Fact]
    public async Task GetCustomerFavoritesAsync_ShouldSkipInactiveCatalogResources()
    {
        var (service, catalog, graph) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1", active: false));
        catalog.ProductsById["product-1"] = Product("product-1", "restaurant-1", available: false);
        graph.FavoriteRestaurants[("customer-1", "restaurant-1")] =
            new FavoriteRestaurantGraphRecord("restaurant-1", DateTimeOffset.UtcNow);
        graph.FavoriteProducts[("customer-1", "product-1")] =
            new FavoriteProductGraphRecord("restaurant-1", "product-1", DateTimeOffset.UtcNow);

        var response = await service.GetCustomerFavoritesAsync("customer-1");

        response.Restaurants.Should().BeEmpty();
        response.Products.Should().BeEmpty();
    }

    private static (FavoriteServiceImpl Service, FakeCatalogClient Catalog, FakeGraphRepository Graph) CreateService()
    {
        var catalog = new FakeCatalogClient();
        var graph = new FakeGraphRepository();

        return (new FavoriteServiceImpl(catalog, graph), catalog, graph);
    }

    private static CatalogRestaurantResponse Restaurant(string id, bool active = true)
    {
        return new CatalogRestaurantResponse
        {
            Id = id,
            Name = $"Restaurant {id}",
            Active = active,
            Logo = new CatalogImageResponse { Url = $"https://images.tap2eat.test/{id}.webp" }
        };
    }

    private static CatalogProductResponse Product(
        string id,
        string restaurantId,
        bool active = true,
        bool available = true)
    {
        return new CatalogProductResponse
        {
            Id = id,
            RestaurantId = restaurantId,
            Name = $"Product {id}",
            Price = 99,
            Active = active,
            Available = available,
            Image = new CatalogImageResponse { Url = $"https://images.tap2eat.test/{id}.webp" }
        };
    }
}
