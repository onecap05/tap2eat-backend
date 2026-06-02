namespace RecommendationService.Dtos.Responses;

public sealed class FavoriteRestaurantResponse
{
    public string RestaurantId { get; set; } = string.Empty;

    public string RestaurantName { get; set; } = string.Empty;

    public string? RestaurantImageUrl { get; set; }

    public bool Available { get; set; }

    public DateTimeOffset CreatedAt { get; set; }
}
