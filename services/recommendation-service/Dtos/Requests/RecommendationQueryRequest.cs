namespace RecommendationService.Dtos.Requests;

public sealed class RecommendationQueryRequest
{
    public double? Lat { get; set; }

    public double? Lng { get; set; }

    public double? RadiusKm { get; set; }
}
