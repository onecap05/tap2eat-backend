using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Domain.Enums;
using OrderService.Exceptions;
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

    public async Task<OrderResponse> GetByIdAsync(
        string id,
        CancellationToken cancellationToken = default)
    {
        var order = await _orderRepository.FindByIdAsync(id, cancellationToken);

        if (order is null)
        {
            throw new OrderNotFoundException(id);
        }

        return OrderMapper.ToResponse(order);
    }

    public async Task<IReadOnlyList<OrderResponse>> GetByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        var orders = await _orderRepository.FindByCustomerAccountIdAsync(customerAccountId, cancellationToken);

        return orders.Select(OrderMapper.ToResponse).ToList();
    }

    public async Task<IReadOnlyList<OrderResponse>> GetByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        var orders = await _orderRepository.FindByRestaurantIdAsync(restaurantId, cancellationToken);

        return orders.Select(OrderMapper.ToResponse).ToList();
    }

    public async Task<OrderResponse> UpdateStatusAsync(
        string id,
        UpdateOrderStatusRequest request,
        CancellationToken cancellationToken = default)
    {
        var order = await _orderRepository.FindByIdAsync(id, cancellationToken);

        if (order is null)
        {
            throw new OrderNotFoundException(id);
        }

        var requestedStatus = request.Status!.Value;

        if (!CanTransition(order.Status, requestedStatus))
        {
            throw new InvalidOrderStatusTransitionException(order.Status, requestedStatus);
        }

        var updatedOrder = await _orderRepository.UpdateStatusAsync(
            id,
            requestedStatus,
            DateTime.UtcNow,
            cancellationToken);

        if (updatedOrder is null)
        {
            throw new OrderNotFoundException(id);
        }

        return OrderMapper.ToResponse(updatedOrder);
    }

    private static bool CanTransition(OrderStatus currentStatus, OrderStatus requestedStatus)
    {
        return currentStatus switch
        {
            OrderStatus.Created => requestedStatus is OrderStatus.Accepted or OrderStatus.Cancelled,
            OrderStatus.Accepted => requestedStatus is OrderStatus.Preparing or OrderStatus.Cancelled,
            OrderStatus.Preparing => requestedStatus is OrderStatus.Ready or OrderStatus.Cancelled,
            OrderStatus.Ready => requestedStatus is OrderStatus.Delivered,
            OrderStatus.Delivered => false,
            OrderStatus.Cancelled => false,
            _ => false
        };
    }
}
