using OrderService.Dtos.Requests;
using OrderService.Integrations.Catalog.Dtos;

namespace OrderService.Integrations.Catalog;

public interface ICatalogClient
{
    Task<ValidateOrderResponse> ValidateOrderAsync(
        CreateOrderRequest request,
        CancellationToken cancellationToken = default);
}
