using RecommendationService.Dtos.Requests;
using RecommendationService.Dtos.Responses;

namespace RecommendationService.Services;

public interface IRecommendationService
{
    Task<IReadOnlyList<BranchRecommendationResponse>> GetNearbyAsync(
        RecommendationQueryRequest query,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<BranchRecommendationResponse>> GetForCustomerAsync(
        string customerAccountId,
        RecommendationQueryRequest query,
        CancellationToken cancellationToken = default);

    Task<RecommendedBranchResponse> GetNearestBranchAsync(
        string restaurantId,
        RecommendationQueryRequest query,
        CancellationToken cancellationToken = default);
}
