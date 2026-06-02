using FluentAssertions;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moq;
using RecommendationService.Controllers;
using RecommendationService.Dtos.Responses;
using RecommendationService.Services;

namespace RecommendationService.Tests.Controllers;

public sealed class FavoritesControllerTests
{
    [Fact]
    public void FavoritesController_ShouldRequireAuthorization()
    {
        typeof(FavoritesController)
            .GetCustomAttributes(typeof(AuthorizeAttribute), inherit: true)
            .Should()
            .NotBeEmpty();
    }

    [Fact]
    public async Task AddRestaurant_ShouldReturnOkResponse()
    {
        var service = new Mock<IFavoriteService>();
        service
            .Setup(item => item.AddRestaurantAsync("customer-1", "restaurant-1", It.IsAny<CancellationToken>()))
            .ReturnsAsync(new FavoriteRestaurantResponse { RestaurantId = "restaurant-1" });
        var controller = new FavoritesController(service.Object);

        var response = await controller.AddRestaurant("restaurant-1", "customer-1", CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>();
    }

    [Fact]
    public async Task RemoveRestaurant_ShouldReturnNoContent()
    {
        var service = new Mock<IFavoriteService>();
        var controller = new FavoritesController(service.Object);

        var response = await controller.RemoveRestaurant("restaurant-1", "customer-1", CancellationToken.None);

        response.Should().BeOfType<NoContentResult>();
        service.Verify(item => item.RemoveRestaurantAsync("customer-1", "restaurant-1", It.IsAny<CancellationToken>()));
    }

    [Fact]
    public async Task AddProduct_ShouldReturnOkResponse()
    {
        var service = new Mock<IFavoriteService>();
        service
            .Setup(item => item.AddProductAsync("customer-1", "product-1", It.IsAny<CancellationToken>()))
            .ReturnsAsync(new FavoriteProductResponse { ProductId = "product-1" });
        var controller = new FavoritesController(service.Object);

        var response = await controller.AddProduct("product-1", "customer-1", CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>();
    }

    [Fact]
    public async Task GetCustomerFavorites_ShouldReturnOkResponse()
    {
        var service = new Mock<IFavoriteService>();
        service
            .Setup(item => item.GetCustomerFavoritesAsync("customer-1", It.IsAny<CancellationToken>()))
            .ReturnsAsync(new CustomerFavoritesResponse());
        var controller = new FavoritesController(service.Object);

        var response = await controller.GetCustomerFavorites("customer-1", CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>();
    }

    [Fact]
    public async Task GetCustomerFavoriteStatus_ShouldPassRestaurantAndProductIds()
    {
        var service = new Mock<IFavoriteService>();
        service
            .Setup(item => item.GetCustomerFavoriteStatusAsync(
                "customer-1",
                It.Is<IReadOnlyList<string>>(ids => ids.Contains("restaurant-1")),
                It.Is<IReadOnlyList<string>>(ids => ids.Contains("product-1")),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(new FavoriteStatusResponse
            {
                RestaurantIds = ["restaurant-1"],
                ProductIds = ["product-1"]
            });
        var controller = new FavoritesController(service.Object);

        var response = await controller.GetCustomerFavoriteStatus(
            "customer-1",
            ["restaurant-1"],
            ["product-1"],
            CancellationToken.None);

        response.Result.Should().BeOfType<OkObjectResult>();
    }

    [Fact]
    public async Task GetFeaturedProduct_WhenMissing_ShouldReturnNotFound()
    {
        var service = new Mock<IFavoriteService>();
        service
            .Setup(item => item.GetFeaturedProductAsync("restaurant-1", It.IsAny<CancellationToken>()))
            .ReturnsAsync((FeaturedProductResponse?)null);
        var controller = new FavoritesController(service.Object);

        var response = await controller.GetFeaturedProduct("restaurant-1", CancellationToken.None);

        response.Result.Should().BeOfType<NotFoundResult>();
    }
}
