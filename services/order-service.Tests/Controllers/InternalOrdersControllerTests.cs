using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using Moq;
using OrderService.Config;
using OrderService.Controllers;
using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Services.Interfaces;

namespace OrderService.Tests.Controllers;

public sealed class InternalOrdersControllerTests
{
    [Fact]
    public async Task GetByRestaurantId_WhenTokenIsMissing_ShouldReturnUnauthorized()
    {
        var service = new Mock<IOrderService>();
        var controller = CreateController(service);

        var result = await controller.GetByRestaurantId(
            "restaurant-1",
            new OrderQueryRequest(),
            CancellationToken.None);

        result.Result.Should().BeOfType<UnauthorizedResult>();
        service.Verify(
            item => item.GetOrderByRestaurantIdAsync(
                It.IsAny<string>(),
                It.IsAny<OrderQueryRequest>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task GetByRestaurantId_WhenTokenIsInvalid_ShouldReturnUnauthorized()
    {
        var service = new Mock<IOrderService>();
        var controller = CreateController(service, "wrong-token");

        var result = await controller.GetByRestaurantId(
            "restaurant-1",
            new OrderQueryRequest(),
            CancellationToken.None);

        result.Result.Should().BeOfType<UnauthorizedResult>();
    }

    [Fact]
    public async Task GetByRestaurantId_WhenTokenIsValid_ShouldReturnOrders()
    {
        var expectedOrders = new[]
        {
            new OrderResponse
            {
                Id = "order-1",
                RestaurantId = "restaurant-1",
                Status = OrderStatus.Created
            }
        };
        var service = new Mock<IOrderService>();
        service
            .Setup(item => item.GetOrderByRestaurantIdAsync(
                "restaurant-1",
                It.Is<OrderQueryRequest>(query => query.Status == OrderStatus.Created),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedOrders);
        var controller = CreateController(service, "internal-token");

        var result = await controller.GetByRestaurantId(
            "restaurant-1",
            new OrderQueryRequest { Status = OrderStatus.Created },
            CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedOrders);
    }

    [Fact]
    public async Task GetByRestaurantId_WhenConfiguredTokenIsBlank_ShouldReturnUnauthorized()
    {
        var service = new Mock<IOrderService>();
        var controller = CreateController(service, "internal-token", configuredToken: "");

        var result = await controller.GetByRestaurantId(
            "restaurant-1",
            new OrderQueryRequest(),
            CancellationToken.None);

        result.Result.Should().BeOfType<UnauthorizedResult>();
    }

    private static InternalOrdersController CreateController(
        Mock<IOrderService> service,
        string? providedToken = null,
        string configuredToken = "internal-token")
    {
        var context = new DefaultHttpContext();

        if (providedToken is not null)
        {
            context.Request.Headers["X-Internal-Service-Token"] = providedToken;
        }

        return new InternalOrdersController(
            service.Object,
            Options.Create(new InternalServiceSettings { Token = configuredToken }))
        {
            ControllerContext = new ControllerContext
            {
                HttpContext = context
            }
        };
    }
}
