using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;

namespace FinanceService.Services.Interfaces;

public interface IPayPalPaymentService
{
    Task<PayPalOrderResponse> CreateOrderAsync(
        Guid paymentId,
        CancellationToken cancellationToken = default);

    Task<PayPalCaptureResponse> CaptureOrderAsync(
        Guid paymentId,
        CapturePayPalOrderRequest request,
        CancellationToken cancellationToken = default);
}
