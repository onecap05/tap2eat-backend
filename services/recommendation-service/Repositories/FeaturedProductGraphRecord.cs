namespace RecommendationService.Repositories;

public sealed record FeaturedProductGraphRecord(
    string ProductId,
    int FavoriteCount);
