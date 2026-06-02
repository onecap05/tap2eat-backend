namespace RecommendationService.Repositories;

public interface IRecommendationGraphRepository
{
    Task InitializeAsync(CancellationToken cancellationToken = default);

    Task UpsertDeliveredOrderAsync(
        DeliveredOrderGraphUpdate update,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<string>> GetPreferredTagsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<string>> GetRecommendedRestaurantIdsByTagsAsync(
        IReadOnlyList<string> tagNames,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<string>> GetAlsoOrderedRestaurantIdsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<FavoriteRestaurantGraphRecord> AddFavoriteRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        DateTimeOffset createdAt,
        CancellationToken cancellationToken = default);

    Task RemoveFavoriteRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<FavoriteProductGraphRecord> AddFavoriteProductAsync(
        string customerAccountId,
        string restaurantId,
        string productId,
        DateTimeOffset createdAt,
        CancellationToken cancellationToken = default);

    Task RemoveFavoriteProductAsync(
        string customerAccountId,
        string productId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<FavoriteRestaurantGraphRecord>> GetFavoriteRestaurantsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<FavoriteProductGraphRecord>> GetFavoriteProductsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<FeaturedProductGraphRecord>> GetFeaturedProductCandidatesAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);
}
