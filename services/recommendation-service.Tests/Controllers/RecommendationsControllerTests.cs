using FluentAssertions;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Microsoft.AspNetCore.Mvc;
using Moq;
using RecommendationService.Controllers;
using RecommendationService.Dtos.Requests;
using RecommendationService.Dtos.Responses;
using RecommendationService.Services;
using System.Net;

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
            .ReturnsAsync([new BranchRecommendationResponse { BranchId = "branch-1", RecommendationType = "NEARBY" }]);

        var controller = new RecommendationsController(service.Object);

        var response = await controller.GetNearby(new RecommendationQueryRequest(), CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>();
    }

    [Fact]
    public async Task GetForCustomer_ShouldReturnOkResponse()
    {
        var expected = new[]
        {
            new BranchRecommendationResponse { BranchId = "branch-1", RecommendationType = "TASTE_BASED" }
        };
        var service = new Mock<IRecommendationService>();
        service
            .Setup(item => item.GetForCustomerAsync(
                "customer-1",
                It.IsAny<RecommendationQueryRequest>(),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(expected);

        var controller = new RecommendationsController(service.Object);

        var response = await controller.GetForCustomer(
            "customer-1",
            new RecommendationQueryRequest(),
            CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expected);
    }

    [Fact]
    public async Task GetCustomerSections_ShouldReturnOkResponse()
    {
        var service = new Mock<IRecommendationService>();
        service
            .Setup(item => item.GetCustomerSectionsAsync(
                "customer-1",
                It.IsAny<RecommendationQueryRequest>(),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(new CustomerRecommendationSectionsResponse
            {
                Nearby =
                [
                    new BranchRecommendationResponse
                    {
                        BranchId = "branch-1",
                        RecommendationType = "NEARBY"
                    }
                ],
                AlsoOrdered = [],
                TasteBased = []
            });

        var controller = new RecommendationsController(service.Object);

        var response = await controller.GetCustomerSections(
            "customer-1",
            new RecommendationQueryRequest(),
            CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>();
    }

    [Fact]
    public async Task GetNearestBranch_ShouldReturnOkResponse()
    {
        var expected = new RecommendedBranchResponse
        {
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            Reason = "Cerca de ti"
        };
        var service = new Mock<IRecommendationService>();
        service
            .Setup(item => item.GetNearestBranchAsync(
                "restaurant-1",
                It.IsAny<RecommendationQueryRequest>(),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(expected);

        var controller = new RecommendationsController(service.Object);

        var response = await controller.GetNearestBranch(
            "restaurant-1",
            new RecommendationQueryRequest(),
            CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expected);
    }

    [Fact]
    public async Task GetCustomerSections_WithoutJwt_ShouldReturnUnauthorized()
    {
        await using var factory = new WebApplicationFactory<Program>()
            .WithWebHostBuilder(builder =>
            {
                builder.ConfigureAppConfiguration((_, configuration) =>
                {
                    configuration.AddInMemoryCollection(new Dictionary<string, string?>
                    {
                        ["RabbitMq:Enabled"] = "false",
                        ["Neo4j:Password"] = "test"
                    });
                });
            });

        using var client = factory.CreateClient();

        var response = await client.GetAsync("/api/recommendations/customers/customer-1/sections");

        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized);
    }
}
