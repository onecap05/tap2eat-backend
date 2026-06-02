using RecommendationService.Dtos.Responses;
using RecommendationService.Exceptions;
using RecommendationService.Integrations.Catalog;
using RecommendationService.Repositories;

namespace RecommendationService.Services;

public sealed class FavoriteServiceImpl : IFavoriteService
{
    private readonly ICatalogClient _catalogClient;
    private readonly IRecommendationGraphRepository _graphRepository;

    public FavoriteServiceImpl(
        ICatalogClient catalogClient,
        IRecommendationGraphRepository graphRepository)
    {
        _catalogClient = catalogClient;
        _graphRepository = graphRepository;
    }

    public async Task<FavoriteRestaurantResponse> AddRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        ValidateCustomerAccountId(customerAccountId);
        ValidateId(restaurantId, "Restaurant id is required.");

        var restaurant = await LoadVisibleRestaurantAsync(restaurantId, cancellationToken)
            ?? throw new RecommendationNotFoundException($"Restaurant '{restaurantId}' was not found.");
        var favorite = await _graphRepository.AddFavoriteRestaurantAsync(
            customerAccountId,
            restaurant.Id,
            DateTimeOffset.UtcNow,
            cancellationToken);

        return ToRestaurantResponse(restaurant, favorite.CreatedAt);
    }

    public async Task RemoveRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        ValidateCustomerAccountId(customerAccountId);
        ValidateId(restaurantId, "Restaurant id is required.");

        await _graphRepository.RemoveFavoriteRestaurantAsync(customerAccountId, restaurantId, cancellationToken);
    }

    public async Task<FavoriteProductResponse> AddProductAsync(
        string customerAccountId,
        string productId,
        CancellationToken cancellationToken = default)
    {
        ValidateCustomerAccountId(customerAccountId);
        ValidateId(productId, "Product id is required.");

        var product = await LoadVisibleProductAsync(productId, cancellationToken)
            ?? throw new RecommendationNotFoundException($"Product '{productId}' was not found.");
        var restaurant = await LoadVisibleRestaurantAsync(product.RestaurantId, cancellationToken)
            ?? throw new RecommendationNotFoundException($"Restaurant '{product.RestaurantId}' was not found.");

        var favorite = await _graphRepository.AddFavoriteProductAsync(
            customerAccountId,
            restaurant.Id,
            product.Id,
            DateTimeOffset.UtcNow,
            cancellationToken);

        return ToProductResponse(product, restaurant, favorite.CreatedAt);
    }

    public async Task RemoveProductAsync(
        string customerAccountId,
        string productId,
        CancellationToken cancellationToken = default)
    {
        ValidateCustomerAccountId(customerAccountId);
        ValidateId(productId, "Product id is required.");

        await _graphRepository.RemoveFavoriteProductAsync(customerAccountId, productId, cancellationToken);
    }

    public async Task<CustomerFavoritesResponse> GetCustomerFavoritesAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return new CustomerFavoritesResponse
        {
            Restaurants = await GetCustomerFavoriteRestaurantsAsync(customerAccountId, cancellationToken),
            Products = await GetCustomerFavoriteProductsAsync(customerAccountId, cancellationToken)
        };
    }

    public async Task<IReadOnlyList<FavoriteRestaurantResponse>> GetCustomerFavoriteRestaurantsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        ValidateCustomerAccountId(customerAccountId);

        var favorites = await _graphRepository.GetFavoriteRestaurantsAsync(customerAccountId, cancellationToken);
        var responses = new List<FavoriteRestaurantResponse>();

        foreach (var favorite in favorites)
        {
            var restaurant = await LoadVisibleRestaurantAsync(favorite.RestaurantId, cancellationToken);
            if (restaurant is null)
            {
                continue;
            }

            responses.Add(ToRestaurantResponse(restaurant, favorite.CreatedAt));
        }

        return responses;
    }

    public async Task<IReadOnlyList<FavoriteProductResponse>> GetCustomerFavoriteProductsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        ValidateCustomerAccountId(customerAccountId);

        var favorites = await _graphRepository.GetFavoriteProductsAsync(customerAccountId, cancellationToken);
        var responses = new List<FavoriteProductResponse>();

        foreach (var favorite in favorites)
        {
            var product = await LoadVisibleProductAsync(favorite.ProductId, cancellationToken);
            if (product is null)
            {
                continue;
            }

            var restaurant = await LoadVisibleRestaurantAsync(product.RestaurantId, cancellationToken);
            if (restaurant is null)
            {
                continue;
            }

            responses.Add(ToProductResponse(product, restaurant, favorite.CreatedAt));
        }

        return responses;
    }

    public async Task<FavoriteStatusResponse> GetCustomerFavoriteStatusAsync(
        string customerAccountId,
        IReadOnlyList<string> restaurantIds,
        IReadOnlyList<string> productIds,
        CancellationToken cancellationToken = default)
    {
        ValidateCustomerAccountId(customerAccountId);

        var requestedRestaurantIds = NormalizeIds(restaurantIds);
        var requestedProductIds = NormalizeIds(productIds);
        var restaurantFavorites = await _graphRepository.GetFavoriteRestaurantsAsync(customerAccountId, cancellationToken);
        var productFavorites = await _graphRepository.GetFavoriteProductsAsync(customerAccountId, cancellationToken);

        return new FavoriteStatusResponse
        {
            RestaurantIds = restaurantFavorites
                .Select(favorite => favorite.RestaurantId)
                .Where(requestedRestaurantIds.Contains)
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .ToArray(),
            ProductIds = productFavorites
                .Select(favorite => favorite.ProductId)
                .Where(requestedProductIds.Contains)
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .ToArray()
        };
    }

    public async Task<FeaturedProductResponse?> GetFeaturedProductAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        ValidateId(restaurantId, "Restaurant id is required.");

        var candidates = await _graphRepository.GetFeaturedProductCandidatesAsync(restaurantId, cancellationToken);

        foreach (var candidate in candidates)
        {
            var product = await LoadVisibleProductAsync(candidate.ProductId, cancellationToken);
            if (product is null || !string.Equals(product.RestaurantId, restaurantId, StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            return new FeaturedProductResponse
            {
                ProductId = product.Id,
                ProductName = product.Name,
                RestaurantId = product.RestaurantId,
                ImageUrl = product.Image?.Url,
                Price = product.Price,
                FavoriteCount = candidate.FavoriteCount
            };
        }

        return null;
    }

    private async Task<CatalogRestaurantResponse?> LoadVisibleRestaurantAsync(
        string restaurantId,
        CancellationToken cancellationToken)
    {
        var restaurant = await _catalogClient.GetRestaurantAsync(restaurantId, cancellationToken);

        return IsVisibleRestaurant(restaurant) ? restaurant : null;
    }

    private async Task<CatalogProductResponse?> LoadVisibleProductAsync(
        string productId,
        CancellationToken cancellationToken)
    {
        var product = await _catalogClient.GetProductAsync(productId, cancellationToken);

        return IsVisibleProduct(product) ? product : null;
    }

    private static FavoriteRestaurantResponse ToRestaurantResponse(
        CatalogRestaurantResponse restaurant,
        DateTimeOffset createdAt)
    {
        return new FavoriteRestaurantResponse
        {
            RestaurantId = restaurant.Id,
            RestaurantName = restaurant.Name,
            RestaurantImageUrl = restaurant.Logo?.Url,
            Available = restaurant.Active != false,
            CreatedAt = createdAt
        };
    }

    private static FavoriteProductResponse ToProductResponse(
        CatalogProductResponse product,
        CatalogRestaurantResponse restaurant,
        DateTimeOffset createdAt)
    {
        return new FavoriteProductResponse
        {
            ProductId = product.Id,
            ProductName = product.Name,
            RestaurantId = product.RestaurantId,
            RestaurantName = restaurant.Name,
            ProductImageUrl = product.Image?.Url,
            Price = product.Price,
            Available = product.Available != false,
            CreatedAt = createdAt
        };
    }

    private static bool IsVisibleRestaurant(CatalogRestaurantResponse? restaurant)
    {
        return restaurant is not null
            && !string.IsNullOrWhiteSpace(restaurant.Id)
            && restaurant.Active != false;
    }

    private static bool IsVisibleProduct(CatalogProductResponse? product)
    {
        return product is not null
            && !string.IsNullOrWhiteSpace(product.Id)
            && !string.IsNullOrWhiteSpace(product.RestaurantId)
            && product.Active != false
            && product.Available != false;
    }

    private static HashSet<string> NormalizeIds(IReadOnlyList<string> ids)
    {
        return ids
            .Where(id => !string.IsNullOrWhiteSpace(id))
            .Select(id => id.Trim())
            .ToHashSet(StringComparer.OrdinalIgnoreCase);
    }

    private static void ValidateCustomerAccountId(string customerAccountId)
    {
        ValidateId(customerAccountId, "Customer account id is required.");
    }

    private static void ValidateId(string value, string message)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new RecommendationValidationException(message);
        }
    }
}
