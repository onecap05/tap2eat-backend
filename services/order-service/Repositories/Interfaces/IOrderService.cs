using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;

namespace OrderService.Services.Interfaces;

public interface IOrderService
{
    Task<OrderResponse> CreateAsync(CreateOrderRequest request, CancellationToken cancellationToken = default);

    Task<OrderResponse?> GetByIdAsync(string id, CancellationToken cancellationToken = default);
}