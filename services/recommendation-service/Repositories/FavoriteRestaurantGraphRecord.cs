namespace RecommendationService.Repositories;

public sealed record FavoriteRestaurantGraphRecord(
    string RestaurantId,
    DateTimeOffset CreatedAt);
