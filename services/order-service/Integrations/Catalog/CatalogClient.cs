using System.Net;
using System.Net.Http.Json;
using OrderService.Dtos.Requests;
using OrderService.Exceptions;
using OrderService.Integrations.Catalog.Dtos;

namespace OrderService.Integrations.Catalog;

public sealed class CatalogClient : ICatalogClient
{
    private const string ValidateOrderPath = "/internal/catalog/orders/validate";

    private readonly HttpClient _httpClient;

    public CatalogClient(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public async Task<ValidateOrderResponse> ValidateOrderAsync(
        CreateOrderRequest request,
        CancellationToken cancellationToken = default)
    {
        var catalogRequest = ToCatalogRequest(request);

        try
        {
            using var response = await _httpClient.PostAsJsonAsync(
                ValidateOrderPath,
                catalogRequest,
                cancellationToken);

            if (response.StatusCode is HttpStatusCode.BadRequest or HttpStatusCode.Conflict or HttpStatusCode.NotFound)
            {
                throw new CatalogValidationException("Catalog rejected the order validation request.");
            }

            if (!response.IsSuccessStatusCode)
            {
                throw new CatalogServiceUnavailableException();
            }

            var validatedOrder = await response.Content.ReadFromJsonAsync<ValidateOrderResponse>(
                cancellationToken);

            if (validatedOrder is null || !validatedOrder.Valid)
            {
                throw new CatalogValidationException("Catalog returned an invalid order validation response.");
            }

            return validatedOrder;
        }
        catch (CatalogValidationException)
        {
            throw;
        }
        catch (CatalogServiceUnavailableException)
        {
            throw;
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            throw new CatalogServiceUnavailableException();
        }
        catch (HttpRequestException)
        {
            throw new CatalogServiceUnavailableException();
        }
    }

    private static ValidateOrderRequest ToCatalogRequest(CreateOrderRequest request)
    {
        return new ValidateOrderRequest
        {
            RestaurantId = request.RestaurantId,
            BranchId = request.BranchId,
            Items = request.Items.Select(ToCatalogItemRequest).ToList()
        };
    }

    private static ValidateOrderItemRequest ToCatalogItemRequest(CreateOrderItemRequest item)
    {
        var selectedModifierOptionIds = item.SelectedModifierOptionIds.Count > 0
            ? item.SelectedModifierOptionIds
            : item.SelectedModifiers
                .Where(modifier => !string.IsNullOrWhiteSpace(modifier.ModifierOptionId))
                .Select(modifier => modifier.ModifierOptionId!)
                .ToList();

        return new ValidateOrderItemRequest
        {
            ProductId = item.ProductId,
            Quantity = item.Quantity,
            SelectedModifierOptionIds = selectedModifierOptionIds
        };
    }
}
