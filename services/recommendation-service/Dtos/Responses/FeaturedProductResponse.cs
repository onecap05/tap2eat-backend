namespace RecommendationService.Dtos.Responses;

public sealed class FeaturedProductResponse
{
    public string ProductId { get; set; } = string.Empty;

    public string ProductName { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string? ImageUrl { get; set; }

    public decimal Price { get; set; }

    public int FavoriteCount { get; set; }
}
