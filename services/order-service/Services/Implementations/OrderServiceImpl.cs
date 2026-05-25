using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Exceptions;
using OrderService.Integrations.Catalog;
using OrderService.Mapping;
using OrderService.Messaging.Publishers;
using OrderService.Repositories.Interfaces;
using OrderService.Services.Interfaces;
using OrderService.Validation;

namespace OrderService.Services.Implementations;

public sealed class OrderServiceImpl : IOrderService
{
    private readonly IOrderRepository _orderRepository;
    private readonly ICatalogClient _catalogClient;
    private readonly IOrderEventPublisher _orderEventPublisher;

    public OrderServiceImpl(
        IOrderRepository orderRepository,
        ICatalogClient catalogClient,
        IOrderEventPublisher orderEventPublisher)
    {
        _orderRepository = orderRepository;
        _catalogClient = catalogClient;
        _orderEventPublisher = orderEventPublisher;
    }

    public async Task<OrderResponse> CreateAsync(
        CreateOrderRequest request,
        CancellationToken cancellationToken = default)
    {
        var validatedOrder = await _catalogClient.ValidateOrderAsync(request, cancellationToken);

        var document = OrderMapper.ToDocument(request, validatedOrder);

        var createdOrder = await _orderRepository.CreateAsync(document, cancellationToken);

        var response = OrderMapper.ToResponse(createdOrder);

        await _orderEventPublisher.PublishOrderCreatedAsync(response, cancellationToken);

        return response;
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

    public async Task<PublicOrderTrackingResponse> GetPublicTrackingAsync(
        string publicTrackingCode,
        CancellationToken cancellationToken = default)
    {
        var order = await _orderRepository.FindByPublicTrackingCodeAsync(
            publicTrackingCode,
            cancellationToken);

        if (order is null)
        {
            throw new OrderNotFoundException(publicTrackingCode);
        }

        return OrderMapper.ToPublicTrackingResponse(order);
    }

    public async Task<IReadOnlyList<OrderResponse>> GetByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return await GetByCustomerAccountIdAsync(
            customerAccountId,
            new OrderQueryRequest(),
            cancellationToken);
    }

    public async Task<IReadOnlyList<OrderResponse>> GetByCustomerAccountIdAsync(
        string customerAccountId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        OrderQueryValidator.Validate(query);

        var orders = await _orderRepository.FindByCustomerAccountIdAsync(
            customerAccountId,
            query,
            cancellationToken);

        return orders
            .Select(OrderMapper.ToResponse)
            .ToList();
    }

    public async Task<IReadOnlyList<OrderResponse>> GetByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        return await GetByRestaurantIdAsync(
            restaurantId,
            new OrderQueryRequest(),
            cancellationToken);
    }

    public async Task<IReadOnlyList<OrderResponse>> GetByRestaurantIdAsync(
        string restaurantId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        OrderQueryValidator.Validate(query);

        var orders = await _orderRepository.FindByRestaurantIdAsync(
            restaurantId,
            query,
            cancellationToken);

        return orders
            .Select(OrderMapper.ToResponse)
            .ToList();
    }

    public async Task<IReadOnlyList<OrderResponse>> GetByBranchIdAsync(
        string branchId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        OrderQueryValidator.Validate(query);

        var orders = await _orderRepository.FindByBranchIdAsync(
            branchId,
            query,
            cancellationToken);

        return orders
            .Select(OrderMapper.ToResponse)
            .ToList();
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
        var previousStatus = order.Status.ToString();

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

        var response = OrderMapper.ToResponse(updatedOrder);

        await _orderEventPublisher.PublishOrderStatusChangedAsync(
            response,
            previousStatus,
            cancellationToken);

        return response;
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
