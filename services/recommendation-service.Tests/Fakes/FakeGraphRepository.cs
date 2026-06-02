using RecommendationService.Repositories;

namespace RecommendationService.Tests.Fakes;

public sealed class FakeGraphRepository : IRecommendationGraphRepository
{
    public List<DeliveredOrderGraphUpdate> SavedUpdates { get; } = [];

    public List<string> PreferredTags { get; } = [];

    public List<string> RecommendedRestaurantIds { get; } = [];

    public List<string> AlsoOrderedRestaurantIds { get; } = [];

    public Dictionary<(string CustomerAccountId, string RestaurantId), FavoriteRestaurantGraphRecord> FavoriteRestaurants { get; } = [];

    public Dictionary<(string CustomerAccountId, string ProductId), FavoriteProductGraphRecord> FavoriteProducts { get; } = [];

    public List<FeaturedProductGraphRecord> FeaturedProductCandidates { get; } = [];

    public Task InitializeAsync(CancellationToken cancellationToken = default)
    {
        return Task.CompletedTask;
    }

    public Task UpsertDeliveredOrderAsync(
        DeliveredOrderGraphUpdate update,
        CancellationToken cancellationToken = default)
    {
        SavedUpdates.Add(update);

        return Task.CompletedTask;
    }

    public Task<IReadOnlyList<string>> GetPreferredTagsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<string>>(PreferredTags);
    }

    public Task<IReadOnlyList<string>> GetRecommendedRestaurantIdsByTagsAsync(
        IReadOnlyList<string> tagNames,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<string>>(RecommendedRestaurantIds);
    }

    public Task<IReadOnlyList<string>> GetAlsoOrderedRestaurantIdsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<string>>(AlsoOrderedRestaurantIds);
    }

    public Task<FavoriteRestaurantGraphRecord> AddFavoriteRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        DateTimeOffset createdAt,
        CancellationToken cancellationToken = default)
    {
        var key = (customerAccountId, restaurantId);

        if (!FavoriteRestaurants.TryGetValue(key, out var favorite))
        {
            favorite = new FavoriteRestaurantGraphRecord(restaurantId, createdAt);
            FavoriteRestaurants[key] = favorite;
        }

        return Task.FromResult(favorite);
    }

    public Task RemoveFavoriteRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        FavoriteRestaurants.Remove((customerAccountId, restaurantId));

        return Task.CompletedTask;
    }

    public Task<FavoriteProductGraphRecord> AddFavoriteProductAsync(
        string customerAccountId,
        string restaurantId,
        string productId,
        DateTimeOffset createdAt,
        CancellationToken cancellationToken = default)
    {
        var key = (customerAccountId, productId);

        if (!FavoriteProducts.TryGetValue(key, out var favorite))
        {
            favorite = new FavoriteProductGraphRecord(restaurantId, productId, createdAt);
            FavoriteProducts[key] = favorite;
        }

        return Task.FromResult(favorite);
    }

    public Task RemoveFavoriteProductAsync(
        string customerAccountId,
        string productId,
        CancellationToken cancellationToken = default)
    {
        FavoriteProducts.Remove((customerAccountId, productId));

        return Task.CompletedTask;
    }

    public Task<IReadOnlyList<FavoriteRestaurantGraphRecord>> GetFavoriteRestaurantsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        var favorites = FavoriteRestaurants
            .Where(item => item.Key.CustomerAccountId == customerAccountId)
            .Select(item => item.Value)
            .ToList();

        return Task.FromResult<IReadOnlyList<FavoriteRestaurantGraphRecord>>(favorites);
    }

    public Task<IReadOnlyList<FavoriteProductGraphRecord>> GetFavoriteProductsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        var favorites = FavoriteProducts
            .Where(item => item.Key.CustomerAccountId == customerAccountId)
            .Select(item => item.Value)
            .ToList();

        return Task.FromResult<IReadOnlyList<FavoriteProductGraphRecord>>(favorites);
    }

    public Task<IReadOnlyList<FeaturedProductGraphRecord>> GetFeaturedProductCandidatesAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<FeaturedProductGraphRecord>>(FeaturedProductCandidates);
    }
}
