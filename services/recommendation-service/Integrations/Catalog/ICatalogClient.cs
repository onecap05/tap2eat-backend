namespace RecommendationService.Integrations.Catalog;

public interface ICatalogClient
{
    Task<IReadOnlyList<CatalogRestaurantResponse>> GetRestaurantsAsync(
        CancellationToken cancellationToken = default);

    Task<CatalogRestaurantResponse?> GetRestaurantAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<CatalogBranchResponse>> GetBranchesByRestaurantAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<CatalogProductResponse?> GetProductAsync(
        string productId,
        CancellationToken cancellationToken = default);
}
