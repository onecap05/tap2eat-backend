using OrderService.Dtos.Requests;
using OrderService.Integrations.Catalog;
using OrderService.Integrations.Catalog.Dtos;

namespace OrderService.Tests.Fakes;

internal sealed class FakeCatalogClient : ICatalogClient
{
    private readonly Func<CreateOrderRequest, CancellationToken, Task<ValidateOrderResponse>> _handler;

    public FakeCatalogClient(ValidateOrderResponse response)
        : this((_, _) => Task.FromResult(response))
    {
    }

    public FakeCatalogClient(Func<CreateOrderRequest, CancellationToken, Task<ValidateOrderResponse>> handler)
    {
        _handler = handler;
    }

    public int Calls { get; private set; }

    public CreateOrderRequest? LastRequest { get; private set; }

    public async Task<ValidateOrderResponse> ValidateOrderAsync(
        CreateOrderRequest request,
        CancellationToken cancellationToken = default)
    {
        Calls++;
        LastRequest = request;

        return await _handler(request, cancellationToken);
    }
}
