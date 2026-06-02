using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RecommendationService.Dtos.Responses;
using RecommendationService.Services;

namespace RecommendationService.Controllers;

[ApiController]
[Authorize]
[Route("api/favorites")]
public sealed class FavoritesController : ControllerBase
{
    private readonly IFavoriteService _favoriteService;

    public FavoritesController(IFavoriteService favoriteService)
    {
        _favoriteService = favoriteService;
    }

    [HttpPost("restaurants/{restaurantId}")]
    public async Task<ActionResult<FavoriteRestaurantResponse>> AddRestaurant(
        string restaurantId,
        [FromQuery] string customerAccountId,
        CancellationToken cancellationToken)
    {
        var favorite = await _favoriteService.AddRestaurantAsync(
            customerAccountId,
            restaurantId,
            cancellationToken);

        return Ok(favorite);
    }

    [HttpDelete("restaurants/{restaurantId}")]
    public async Task<IActionResult> RemoveRestaurant(
        string restaurantId,
        [FromQuery] string customerAccountId,
        CancellationToken cancellationToken)
    {
        await _favoriteService.RemoveRestaurantAsync(customerAccountId, restaurantId, cancellationToken);

        return NoContent();
    }

    [HttpPost("products/{productId}")]
    public async Task<ActionResult<FavoriteProductResponse>> AddProduct(
        string productId,
        [FromQuery] string customerAccountId,
        CancellationToken cancellationToken)
    {
        var favorite = await _favoriteService.AddProductAsync(
            customerAccountId,
            productId,
            cancellationToken);

        return Ok(favorite);
    }

    [HttpDelete("products/{productId}")]
    public async Task<IActionResult> RemoveProduct(
        string productId,
        [FromQuery] string customerAccountId,
        CancellationToken cancellationToken)
    {
        await _favoriteService.RemoveProductAsync(customerAccountId, productId, cancellationToken);

        return NoContent();
    }

    [HttpGet("customers/{customerAccountId}")]
    public async Task<ActionResult<CustomerFavoritesResponse>> GetCustomerFavorites(
        string customerAccountId,
        CancellationToken cancellationToken)
    {
        var favorites = await _favoriteService.GetCustomerFavoritesAsync(customerAccountId, cancellationToken);

        return Ok(favorites);
    }

    [HttpGet("customers/{customerAccountId}/restaurants")]
    public async Task<ActionResult<IReadOnlyList<FavoriteRestaurantResponse>>> GetCustomerFavoriteRestaurants(
        string customerAccountId,
        CancellationToken cancellationToken)
    {
        var favorites = await _favoriteService.GetCustomerFavoriteRestaurantsAsync(
            customerAccountId,
            cancellationToken);

        return Ok(favorites);
    }

    [HttpGet("customers/{customerAccountId}/products")]
    public async Task<ActionResult<IReadOnlyList<FavoriteProductResponse>>> GetCustomerFavoriteProducts(
        string customerAccountId,
        CancellationToken cancellationToken)
    {
        var favorites = await _favoriteService.GetCustomerFavoriteProductsAsync(
            customerAccountId,
            cancellationToken);

        return Ok(favorites);
    }

    [HttpGet("customers/{customerAccountId}/status")]
    public async Task<ActionResult<FavoriteStatusResponse>> GetCustomerFavoriteStatus(
        string customerAccountId,
        [FromQuery] string[]? restaurantIds,
        [FromQuery] string[]? productIds,
        CancellationToken cancellationToken)
    {
        var status = await _favoriteService.GetCustomerFavoriteStatusAsync(
            customerAccountId,
            restaurantIds ?? [],
            productIds ?? [],
            cancellationToken);

        return Ok(status);
    }

    [HttpGet("restaurants/{restaurantId}/featured-product")]
    public async Task<ActionResult<FeaturedProductResponse>> GetFeaturedProduct(
        string restaurantId,
        CancellationToken cancellationToken)
    {
        var product = await _favoriteService.GetFeaturedProductAsync(restaurantId, cancellationToken);

        return product is null ? NotFound() : Ok(product);
    }
}
