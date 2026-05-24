namespace RecommendationService.Services;

public interface ILocationDistanceService
{
    bool HasValidLocation(double? lat, double? lng);

    double CalculateDistanceKm(double originLat, double originLng, double destinationLat, double destinationLng);

    double NormalizeRadiusKm(double? radiusKm);
}
