using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Mapping;
using OrderService.Repositories.Interfaces;
using OrderService.Services.Interfaces;

namespace OrderService.Services.Implementations;

public sealed class OrderServiceImpl : IOrderService
{
    private readonly IOrderRepository _orderRepository;

    public OrderServiceImpl(IOrderRepository orderRepository)
    {
        _orderRepository = orderRepository;
    }

    public async Task<OrderResponse> CreateAsync(
        CreateOrderRequest request,
        CancellationToken cancellationToken = default)
    {
        var document = OrderMapper.ToDocument(request);
        var createdOrder = await _orderRepository.CreateAsync(document, cancellationToken);

        return OrderMapper.ToResponse(createdOrder);
    }

    public async Task<OrderResponse?> GetByIdAsync(
        string id,
        CancellationToken cancellationToken = default)
    {
        var order = await _orderRepository.FindByIdAsync(id, cancellationToken);

        return order is null
            ? null
            : OrderMapper.ToResponse(order);
    }
}