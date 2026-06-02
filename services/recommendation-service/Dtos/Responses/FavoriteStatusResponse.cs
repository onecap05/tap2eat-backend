namespace RecommendationService.Dtos.Responses;

public sealed class FavoriteStatusResponse
{
    public IReadOnlyList<string> RestaurantIds { get; set; } = [];

    public IReadOnlyList<string> ProductIds { get; set; } = [];
}
