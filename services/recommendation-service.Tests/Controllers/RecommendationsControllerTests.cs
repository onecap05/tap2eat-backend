using FluentAssertions;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moq;
using RecommendationService.Controllers;
using RecommendationService.Dtos.Requests;
using RecommendationService.Dtos.Responses;
using RecommendationService.Services;

namespace RecommendationService.Tests.Controllers;

public sealed class RecommendationsControllerTests
{
    [Fact]
    public void RecommendationsController_ShouldRequireAuthorization()
    {
        typeof(RecommendationsController)
            .GetCustomAttributes(typeof(AuthorizeAttribute), inherit: true)
            .Should()
            .NotBeEmpty();
    }

    [Fact]
    public async Task GetNearby_ShouldReturnOkResponse()
    {
        var service = new Mock<IRecommendationService>();
        service
            .Setup(item => item.GetNearbyAsync(It.IsAny<RecommendationQueryRequest>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync([new BranchRecommendationResponse { BranchId = "branch-1" }]);

        var controller = new RecommendationsController(service.Object);

        var response = await controller.GetNearby(new RecommendationQueryRequest(), CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>();
    }
}
