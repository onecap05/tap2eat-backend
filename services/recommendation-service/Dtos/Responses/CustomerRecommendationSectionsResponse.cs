namespace RecommendationService.Dtos.Responses;

public sealed class CustomerRecommendationSectionsResponse
{
    public IReadOnlyList<BranchRecommendationResponse> Nearby { get; set; } = [];

    public IReadOnlyList<BranchRecommendationResponse> AlsoOrdered { get; set; } = [];

    public IReadOnlyList<BranchRecommendationResponse> TasteBased { get; set; } = [];
}
