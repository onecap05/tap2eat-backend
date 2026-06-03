using FinanceService.Controllers;
using FluentAssertions;
using Microsoft.AspNetCore.Mvc;

namespace FinanceService.Tests.Controllers;

public sealed class HealthControllerTests
{
    [Fact]
    public void Health_ReturnsServiceStatus()
    {
        var controller = new HealthController();

        var result = controller.Health();

        var okResult = result.Should().BeOfType<OkObjectResult>().Subject;
        okResult.Value.Should().BeEquivalentTo(new
        {
            service = "finance-service",
            status = "UP"
        });
    }
}
