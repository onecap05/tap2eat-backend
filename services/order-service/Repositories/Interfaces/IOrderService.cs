using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;

namespace OrderService.Services.Interfaces;

public interface IOrderService
{
    Task<OrderResponse> CreateOrderAsync(CreateOrderRequest request, CancellationToken cancellationToken = default);

    Task<OrderResponse> GetOrderByIdAsync(string id, CancellationToken cancellationToken = default);

    Task<PublicOrderTrackingResponse> GetOrderPublicTrackingAsync(
        string publicTrackingCode,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetOrderByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetOrderByCustomerAccountIdAsync(
        string customerAccountId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetOrderByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetOrderByRestaurantIdAsync(
        string restaurantId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetOrderByBranchIdAsync(
        string branchId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default);

    Task<OrderResponse> UpdateStatusAsync(
        string id,
        UpdateOrderStatusRequest request,
        CancellationToken cancellationToken = default);
}
