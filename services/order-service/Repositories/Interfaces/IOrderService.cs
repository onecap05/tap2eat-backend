using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;

namespace OrderService.Services.Interfaces;

public interface IOrderService
{
    Task<OrderResponse> CreateAsync(CreateOrderRequest request, CancellationToken cancellationToken = default);

    Task<OrderResponse> GetByIdAsync(string id, CancellationToken cancellationToken = default);

    Task<PublicOrderTrackingResponse> GetPublicTrackingAsync(
        string publicTrackingCode,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetByCustomerAccountIdAsync(
        string customerAccountId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetByRestaurantIdAsync(
        string restaurantId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderResponse>> GetByBranchIdAsync(
        string branchId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default);

    Task<OrderResponse> UpdateStatusAsync(
        string id,
        UpdateOrderStatusRequest request,
        CancellationToken cancellationToken = default);
}
