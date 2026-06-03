using FluentAssertions;
using Microsoft.AspNetCore.Mvc;
using OrderService.Controllers;

namespace OrderService.Tests.Controllers;

public sealed class HealthControllerTests
{
    [Fact]
    public void Health_ShouldReturnServiceStatus()
    {
        var controller = new HealthController();

        var result = controller.Health();

        result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().BeEquivalentTo(new
            {
                service = "order-service",
                status = "UP"
            });
    }
}
