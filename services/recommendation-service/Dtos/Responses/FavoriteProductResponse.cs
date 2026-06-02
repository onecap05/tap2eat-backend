namespace RecommendationService.Dtos.Responses;

public sealed class FavoriteProductResponse
{
    public string ProductId { get; set; } = string.Empty;

    public string ProductName { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string RestaurantName { get; set; } = string.Empty;

    public string? ProductImageUrl { get; set; }

    public decimal Price { get; set; }

    public bool Available { get; set; }

    public DateTimeOffset CreatedAt { get; set; }
}
