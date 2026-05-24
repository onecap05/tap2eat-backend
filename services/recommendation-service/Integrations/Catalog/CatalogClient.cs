using System.Net;
using System.Net.Http.Json;

namespace RecommendationService.Integrations.Catalog;

public sealed class CatalogClient : ICatalogClient
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<CatalogClient> _logger;

    public CatalogClient(HttpClient httpClient, ILogger<CatalogClient> logger)
    {
        _httpClient = httpClient;
        _logger = logger;
    }

    public async Task<IReadOnlyList<CatalogRestaurantResponse>> GetRestaurantsAsync(
        CancellationToken cancellationToken = default)
    {
        var restaurants = await _httpClient.GetFromJsonAsync<List<CatalogRestaurantResponse>>(
            "/api/customer/restaurants",
            cancellationToken);

        return restaurants ?? [];
    }

    public async Task<CatalogRestaurantResponse?> GetRestaurantAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        try
        {
            return await _httpClient.GetFromJsonAsync<CatalogRestaurantResponse>(
                $"/api/customer/restaurants/{Uri.EscapeDataString(restaurantId)}",
                cancellationToken);
        }
        catch (HttpRequestException exception) when (exception.StatusCode == HttpStatusCode.NotFound)
        {
            return null;
        }
    }

    public async Task<IReadOnlyList<CatalogBranchResponse>> GetBranchesByRestaurantAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        try
        {
            var branches = await _httpClient.GetFromJsonAsync<List<CatalogBranchResponse>>(
                $"/api/customer/restaurants/{Uri.EscapeDataString(restaurantId)}/branches",
                cancellationToken);

            return branches ?? [];
        }
        catch (HttpRequestException exception)
        {
            _logger.LogWarning(
                exception,
                "Could not load branches for restaurant {RestaurantId}.",
                restaurantId);

            return [];
        }
    }

    public async Task<CatalogProductResponse?> GetProductAsync(
        string productId,
        CancellationToken cancellationToken = default)
    {
        try
        {
            return await _httpClient.GetFromJsonAsync<CatalogProductResponse>(
                $"/api/customer/products/{Uri.EscapeDataString(productId)}",
                cancellationToken);
        }
        catch (HttpRequestException exception) when (exception.StatusCode == HttpStatusCode.NotFound)
        {
            return null;
        }
    }
}
