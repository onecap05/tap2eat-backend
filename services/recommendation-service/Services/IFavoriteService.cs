using RecommendationService.Dtos.Responses;

namespace RecommendationService.Services;

public interface IFavoriteService
{
    Task<FavoriteRestaurantResponse> AddRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task RemoveRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<FavoriteProductResponse> AddProductAsync(
        string customerAccountId,
        string productId,
        CancellationToken cancellationToken = default);

    Task RemoveProductAsync(
        string customerAccountId,
        string productId,
        CancellationToken cancellationToken = default);

    Task<CustomerFavoritesResponse> GetCustomerFavoritesAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<FavoriteRestaurantResponse>> GetCustomerFavoriteRestaurantsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<FavoriteProductResponse>> GetCustomerFavoriteProductsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<FavoriteStatusResponse> GetCustomerFavoriteStatusAsync(
        string customerAccountId,
        IReadOnlyList<string> restaurantIds,
        IReadOnlyList<string> productIds,
        CancellationToken cancellationToken = default);

    Task<FeaturedProductResponse?> GetFeaturedProductAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);
}
