using FluentAssertions;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RecommendationService.Controllers;

namespace RecommendationService.Tests.Controllers;

public sealed class HealthControllerTests
{
    [Fact]
    public void HealthController_ShouldAllowAnonymous()
    {
        typeof(HealthController)
            .GetCustomAttributes(typeof(AllowAnonymousAttribute), inherit: true)
            .Should()
            .NotBeEmpty();
    }

    [Fact]
    public void Health_ShouldReturnServiceStatus()
    {
        var controller = new HealthController();

        var result = controller.Health();

        result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().BeEquivalentTo(new
            {
                service = "recommendation-service",
                status = "UP"
            });
    }
}
