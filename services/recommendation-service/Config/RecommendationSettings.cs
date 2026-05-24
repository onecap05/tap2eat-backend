namespace RecommendationService.Config;

public sealed class RecommendationSettings
{
    public const string SectionName = "Recommendations";

    public double DefaultRadiusKm { get; set; } = 5;

    public double MaxRadiusKm { get; set; } = 25;
}
