namespace RecommendationService.Dtos.Responses;

public class BranchRecommendationResponse
{
    public string RestaurantId { get; set; } = string.Empty;

    public string RestaurantName { get; set; } = string.Empty;

    public string? RestaurantImageUrl { get; set; }

    public string BranchId { get; set; } = string.Empty;

    public string BranchName { get; set; } = string.Empty;

    public string? BranchAddress { get; set; }

    public double? Latitude { get; set; }

    public double? Longitude { get; set; }

    public double? DistanceKm { get; set; }

    public string Reason { get; set; } = string.Empty;

    public double Score { get; set; }

    public string? Warning { get; set; }

    public string RecommendationType { get; set; } = string.Empty;
}
