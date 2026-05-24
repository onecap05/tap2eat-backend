using Microsoft.Extensions.Options;
using RecommendationService.Config;

namespace RecommendationService.Services;

public sealed class HaversineDistanceService : ILocationDistanceService
{
    private const double EarthRadiusKm = 6371.0088;
    private readonly RecommendationSettings _settings;

    public HaversineDistanceService(IOptions<RecommendationSettings> options)
    {
        _settings = options.Value;
    }

    public bool HasValidLocation(double? lat, double? lng)
    {
        return lat is >= -90 and <= 90 && lng is >= -180 and <= 180;
    }

    public double CalculateDistanceKm(
        double originLat,
        double originLng,
        double destinationLat,
        double destinationLng)
    {
        var originLatRadians = ToRadians(originLat);
        var destinationLatRadians = ToRadians(destinationLat);
        var latDelta = ToRadians(destinationLat - originLat);
        var lngDelta = ToRadians(destinationLng - originLng);

        var haversine = Math.Sin(latDelta / 2) * Math.Sin(latDelta / 2)
            + Math.Cos(originLatRadians) * Math.Cos(destinationLatRadians)
            * Math.Sin(lngDelta / 2) * Math.Sin(lngDelta / 2);

        var centralAngle = 2 * Math.Atan2(Math.Sqrt(haversine), Math.Sqrt(1 - haversine));

        return Math.Round(EarthRadiusKm * centralAngle, 2, MidpointRounding.AwayFromZero);
    }

    public double NormalizeRadiusKm(double? radiusKm)
    {
        if (radiusKm is null or <= 0)
        {
            return _settings.DefaultRadiusKm;
        }

        return Math.Min(radiusKm.Value, _settings.MaxRadiusKm);
    }

    private static double ToRadians(double degrees)
    {
        return degrees * Math.PI / 180;
    }
}
