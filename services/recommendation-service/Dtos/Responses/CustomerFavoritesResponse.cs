namespace RecommendationService.Dtos.Responses;

public sealed class CustomerFavoritesResponse
{
    public IReadOnlyList<FavoriteRestaurantResponse> Restaurants { get; set; } = [];

    public IReadOnlyList<FavoriteProductResponse> Products { get; set; } = [];
}
