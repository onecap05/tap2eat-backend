namespace RecommendationService.Repositories;

public sealed record FavoriteProductGraphRecord(
    string RestaurantId,
    string ProductId,
    DateTimeOffset CreatedAt);
