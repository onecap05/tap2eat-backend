using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Messaging.Events;

namespace FinanceService.Services.Interfaces;

public interface IPaymentService
{
    Task<PaymentResponse> CreatePendingPaymentFromOrderAsync(
        OrderCreatedEvent orderCreatedEvent,
        CancellationToken cancellationToken = default);

    Task HandleOrderStatusChangedAsync(
        OrderStatusChangedEvent orderStatusChangedEvent,
        CancellationToken cancellationToken = default);

    Task<PaymentResponse> GetByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<PaymentResponse> GetByOrderIdAsync(string orderId, CancellationToken cancellationToken = default);

    Task<IReadOnlyList<PaymentResponse>> GetByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<PaymentResponse>> GetByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<PaymentResponse> ApproveAsync(
        Guid id,
        ApprovePaymentRequest request,
        CancellationToken cancellationToken = default);

    Task<PaymentResponse> RejectAsync(
        Guid id,
        RejectPaymentRequest request,
        CancellationToken cancellationToken = default);

    Task<PaymentResponse> CancelAsync(Guid id, CancellationToken cancellationToken = default);
}
