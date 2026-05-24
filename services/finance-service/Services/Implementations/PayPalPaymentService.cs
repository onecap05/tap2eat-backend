using FinanceService.Config;
using FinanceService.Domain.Entities;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Exceptions;
using FinanceService.Messaging.Publishers;
using FinanceService.Repositories.Interfaces;
using FinanceService.Services.Interfaces;
using Microsoft.Extensions.Options;

namespace FinanceService.Services.Implementations;

public sealed class PayPalPaymentService : IPayPalPaymentService
{
    private const string PayPalProvider = "PAYPAL";
    private const string CompletedStatus = "COMPLETED";

    private readonly IPaymentRepository _paymentRepository;
    private readonly IPayPalClient _payPalClient;
    private readonly IPaymentEventPublisher _paymentEventPublisher;
    private readonly PayPalSettings _settings;

    public PayPalPaymentService(
        IPaymentRepository paymentRepository,
        IPayPalClient payPalClient,
        IPaymentEventPublisher paymentEventPublisher,
        IOptions<PayPalSettings> settings)
    {
        _paymentRepository = paymentRepository;
        _payPalClient = payPalClient;
        _paymentEventPublisher = paymentEventPublisher;
        _settings = settings.Value;
    }

    public async Task<PayPalOrderResponse> CreateOrderAsync(
        Guid paymentId,
        CancellationToken cancellationToken = default)
    {
        var payment = await GetPaymentOrThrowAsync(paymentId, cancellationToken);
        EnsurePending(payment, "create a PayPal order");

        var currency = string.IsNullOrWhiteSpace(payment.Currency)
            ? _settings.Currency
            : payment.Currency;
        var paypalOrderId = await _payPalClient.CreateOrderAsync(
            payment.Amount,
            currency,
            payment.OrderId,
            cancellationToken);

        payment.Provider = PayPalProvider;
        payment.ProviderReference = paypalOrderId;
        payment.UpdatedAt = DateTime.UtcNow;

        var updatedPayment = await _paymentRepository.UpdateAsync(payment, cancellationToken);

        return new PayPalOrderResponse
        {
            PaymentId = updatedPayment.Id,
            PaypalOrderId = paypalOrderId,
            Status = updatedPayment.Status.ToString(),
            Amount = updatedPayment.Amount,
            Currency = currency
        };
    }

    public async Task<PayPalCaptureResponse> CaptureOrderAsync(
        Guid paymentId,
        CapturePayPalOrderRequest request,
        CancellationToken cancellationToken = default)
    {
        var payment = await GetPaymentOrThrowAsync(paymentId, cancellationToken);
        EnsurePending(payment, "capture a PayPal order");

        var paypalOrderId = string.IsNullOrWhiteSpace(request.PaypalOrderId)
            ? payment.ProviderReference
            : request.PaypalOrderId.Trim();

        if (string.IsNullOrWhiteSpace(paypalOrderId))
        {
            throw new FinanceValidationException("PayPal order id is required.");
        }

        var captureResult = await _payPalClient.CaptureOrderAsync(paypalOrderId, cancellationToken);

        if (!string.Equals(captureResult.Status, CompletedStatus, StringComparison.OrdinalIgnoreCase))
        {
            throw new PayPalPaymentException(
                $"PayPal capture was not completed. Status: {captureResult.Status}.",
                StatusCodes.Status409Conflict);
        }

        var now = DateTime.UtcNow;
        payment.Status = PaymentStatus.Approved;
        payment.Provider = PayPalProvider;
        payment.ProviderReference = captureResult.ProviderReference ?? paypalOrderId;
        payment.ApprovedAt = now;
        payment.UpdatedAt = now;

        var updatedPayment = await _paymentRepository.UpdateAsync(payment, cancellationToken);
        await _paymentEventPublisher.PublishPaymentApprovedAsync(updatedPayment, cancellationToken);

        return new PayPalCaptureResponse
        {
            PaymentId = updatedPayment.Id,
            PaypalOrderId = captureResult.PayPalOrderId,
            CaptureId = captureResult.CaptureId,
            PaymentStatus = updatedPayment.Status,
            ProviderReference = updatedPayment.ProviderReference
        };
    }

    private async Task<Payment> GetPaymentOrThrowAsync(
        Guid paymentId,
        CancellationToken cancellationToken)
    {
        var payment = await _paymentRepository.FindByIdAsync(paymentId, cancellationToken);

        if (payment is null)
        {
            throw new PaymentNotFoundException(paymentId.ToString());
        }

        return payment;
    }

    private static void EnsurePending(Payment payment, string action)
    {
        if (payment.Status != PaymentStatus.Pending)
        {
            throw new InvalidPaymentStatusTransitionException(payment.Status, PaymentStatus.Approved);
        }
    }
}
