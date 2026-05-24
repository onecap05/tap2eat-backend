namespace RecommendationService.Exceptions;

public sealed class RecommendationValidationException : RecommendationException
{
    public RecommendationValidationException(string message)
        : base("RECOMMENDATION_VALIDATION_ERROR", message, StatusCodes.Status400BadRequest)
    {
    }
}
