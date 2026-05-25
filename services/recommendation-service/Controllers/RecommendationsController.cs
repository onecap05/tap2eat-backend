using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RecommendationService.Dtos.Requests;
using RecommendationService.Dtos.Responses;
using RecommendationService.Services;

namespace RecommendationService.Controllers;

[ApiController]
[Authorize]
[Route("api/recommendations")]
public sealed class RecommendationsController : ControllerBase
{
    private readonly IRecommendationService _recommendationService;

    public RecommendationsController(IRecommendationService recommendationService)
    {
        _recommendationService = recommendationService;
    }

    [HttpGet("nearby")]
    public async Task<ActionResult<IReadOnlyList<BranchRecommendationResponse>>> GetNearby(
        [FromQuery] RecommendationQueryRequest query,
        CancellationToken cancellationToken)
    {
        var recommendations = await _recommendationService.GetNearbyAsync(query, cancellationToken);

        return Ok(recommendations);
    }

    [HttpGet("customers/{customerAccountId}")]
    public async Task<ActionResult<IReadOnlyList<BranchRecommendationResponse>>> GetForCustomer(
        string customerAccountId,
        [FromQuery] RecommendationQueryRequest query,
        CancellationToken cancellationToken)
    {
        var recommendations = await _recommendationService.GetForCustomerAsync(
            customerAccountId,
            query,
            cancellationToken);

        return Ok(recommendations);
    }

    [HttpGet("customers/{customerAccountId}/sections")]
    public async Task<ActionResult<CustomerRecommendationSectionsResponse>> GetCustomerSections(
        string customerAccountId,
        [FromQuery] RecommendationQueryRequest query,
        CancellationToken cancellationToken)
    {
        var recommendations = await _recommendationService.GetCustomerSectionsAsync(
            customerAccountId,
            query,
            cancellationToken);

        return Ok(recommendations);
    }

    [HttpGet("restaurants/{restaurantId}/nearest-branch")]
    public async Task<ActionResult<RecommendedBranchResponse>> GetNearestBranch(
        string restaurantId,
        [FromQuery] RecommendationQueryRequest query,
        CancellationToken cancellationToken)
    {
        var recommendation = await _recommendationService.GetNearestBranchAsync(
            restaurantId,
            query,
            cancellationToken);

        return Ok(recommendation);
    }
}
