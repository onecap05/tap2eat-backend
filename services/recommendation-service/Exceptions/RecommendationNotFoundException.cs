namespace RecommendationService.Exceptions;

public sealed class RecommendationNotFoundException : RecommendationException
{
    public RecommendationNotFoundException(string message)
        : base("RECOMMENDATION_NOT_FOUND", message, StatusCodes.Status404NotFound)
    {
    }
}
