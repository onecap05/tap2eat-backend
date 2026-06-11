using OrderService.Domain.Enums;
using OrderService.Domain.Documents;
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
    private const int MaxEstimatedPreparationMinutes = 240;

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

    public async Task<OrderResponse> CreateOrderAsync(
        CreateOrderRequest request,
        CancellationToken cancellationToken = default)
    {
        var validatedOrder = await _catalogClient.ValidateOrderAsync(request, cancellationToken);

        var document = OrderMapper.ToDocument(request, validatedOrder);
        ApplyPaymentDetails(request, document);

        var createdOrder = await _orderRepository.CreateAsync(document, cancellationToken);

        var response = OrderMapper.ToResponse(createdOrder);

        await _orderEventPublisher.PublishOrderCreatedAsync(response, cancellationToken);

        return response;
    }

    public async Task<OrderResponse> GetOrderByIdAsync(
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

    public async Task<PublicOrderTrackingResponse> GetOrderPublicTrackingAsync(
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

    public async Task<IReadOnlyList<OrderResponse>> GetOrderByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return await GetOrderByCustomerAccountIdAsync(
            customerAccountId,
            new OrderQueryRequest(),
            cancellationToken);
    }

    public async Task<IReadOnlyList<OrderResponse>> GetOrderByCustomerAccountIdAsync(
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

    public async Task<IReadOnlyList<OrderResponse>> GetOrderByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        return await GetOrderByRestaurantIdAsync(
            restaurantId,
            new OrderQueryRequest(),
            cancellationToken);
    }

    public async Task<IReadOnlyList<OrderResponse>> GetOrderByRestaurantIdAsync(
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

    public async Task<IReadOnlyList<OrderResponse>> GetOrderByBranchIdAsync(
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
        var updatedAt = DateTime.UtcNow;
        var requestedPreparationMinutes = GetEstimatedPreparationMinutes(request, requestedStatus);
        var estimatedPreparationMinutes = requestedPreparationMinutes ?? order.EstimatedPreparationMinutes;
        var estimatedReadyAt = requestedPreparationMinutes.HasValue
            ? updatedAt.AddMinutes(requestedPreparationMinutes.Value)
            : order.EstimatedReadyAt;

        if (!CanTransition(order.Status, requestedStatus))
        {
            throw new InvalidOrderStatusTransitionException(order.Status, requestedStatus);
        }

        var updatedOrder = await _orderRepository.UpdateStatusAsync(
            id,
            requestedStatus,
            estimatedPreparationMinutes,
            estimatedReadyAt,
            updatedAt,
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

    private static void ApplyPaymentDetails(CreateOrderRequest request, OrderDocument document)
    {
        if (request.PaymentMethod is not PaymentMethod.Cash)
        {
            document.PaymentMethod = request.PaymentMethod;
            document.CashPaymentType = null;
            document.CashAmountProvided = null;
            document.EstimatedChange = null;
            return;
        }

        if (!request.CashPaymentType.HasValue)
        {
            throw new OrderValidationException("Cash payment type is required for cash orders.");
        }

        document.PaymentMethod = PaymentMethod.Cash;
        document.CashPaymentType = request.CashPaymentType.Value;

        if (request.CashPaymentType is CashPaymentType.UnknownAmount)
        {
            if (request.CashAmountProvided.HasValue || request.EstimatedChange.HasValue)
            {
                throw new OrderValidationException("Cash amount and estimated change must be empty when the customer does not know the payment amount.");
            }

            document.CashAmountProvided = null;
            document.EstimatedChange = null;
            return;
        }

        if (!request.CashAmountProvided.HasValue)
        {
            throw new OrderValidationException("Cash amount is required when the customer will pay with a specific amount.");
        }

        if (request.CashAmountProvided.Value < document.Total)
        {
            throw new OrderValidationException("Cash amount must be greater than or equal to the order total.");
        }

        document.CashAmountProvided = request.CashAmountProvided.Value;
        document.EstimatedChange = request.CashAmountProvided.Value - document.Total;
    }

    private static int? GetEstimatedPreparationMinutes(
        UpdateOrderStatusRequest request,
        OrderStatus requestedStatus)
    {
        if (!request.EstimatedPreparationMinutes.HasValue)
        {
            return null;
        }

        if (requestedStatus is not OrderStatus.Accepted)
        {
            throw new OrderValidationException("Estimated preparation time can only be provided when accepting an order.");
        }

        if (request.EstimatedPreparationMinutes <= 0)
        {
            throw new OrderValidationException("Estimated preparation time must be greater than zero.");
        }

        if (request.EstimatedPreparationMinutes > MaxEstimatedPreparationMinutes)
        {
            throw new OrderValidationException($"Estimated preparation time cannot exceed {MaxEstimatedPreparationMinutes} minutes.");
        }

        return request.EstimatedPreparationMinutes.Value;
    }
}
