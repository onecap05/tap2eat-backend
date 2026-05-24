using RecommendationService.Integrations.Catalog;

namespace RecommendationService.Tests.Fakes;

public sealed class FakeCatalogClient : ICatalogClient
{
    public List<CatalogRestaurantResponse> Restaurants { get; } = [];

    public Dictionary<string, List<CatalogBranchResponse>> BranchesByRestaurantId { get; } = new();

    public Dictionary<string, CatalogProductResponse> ProductsById { get; } = new();

    public Task<IReadOnlyList<CatalogRestaurantResponse>> GetRestaurantsAsync(
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<CatalogRestaurantResponse>>(Restaurants);
    }

    public Task<CatalogRestaurantResponse?> GetRestaurantAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        var restaurant = Restaurants.FirstOrDefault(item => item.Id == restaurantId);

        return Task.FromResult(restaurant);
    }

    public Task<IReadOnlyList<CatalogBranchResponse>> GetBranchesByRestaurantAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        BranchesByRestaurantId.TryGetValue(restaurantId, out var branches);

        return Task.FromResult<IReadOnlyList<CatalogBranchResponse>>(branches ?? []);
    }

    public Task<CatalogProductResponse?> GetProductAsync(
        string productId,
        CancellationToken cancellationToken = default)
    {
        ProductsById.TryGetValue(productId, out var product);

        return Task.FromResult(product);
    }
}
