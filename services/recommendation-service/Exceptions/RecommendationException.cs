namespace RecommendationService.Exceptions;

public class RecommendationException : Exception
{
    public RecommendationException(string code, string message, int statusCode)
        : base(message)
    {
        Code = code;
        StatusCode = statusCode;
    }

    public string Code { get; }

    public int StatusCode { get; }
}
