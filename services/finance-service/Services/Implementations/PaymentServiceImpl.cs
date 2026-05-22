using FinanceService.Domain.Entities;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Exceptions;
using FinanceService.Mapping;
using FinanceService.Messaging.Events;
using FinanceService.Repositories.Interfaces;
using FinanceService.Services.Interfaces;

namespace FinanceService.Services.Implementations;

public sealed class PaymentServiceImpl : IPaymentService
{
    private const string SimulatedProvider = "SIMULATED";
    private const string CancelledOrderStatus = "Cancelled";

    private readonly IPaymentRepository _paymentRepository;
    private readonly ILogger<PaymentServiceImpl> _logger;

    public PaymentServiceImpl(
        IPaymentRepository paymentRepository,
        ILogger<PaymentServiceImpl> logger)
    {
        _paymentRepository = paymentRepository;
        _logger = logger;
    }

    public async Task<PaymentResponse> CreatePendingPaymentFromOrderAsync(
        OrderCreatedEvent orderCreatedEvent,
        CancellationToken cancellationToken = default)
    {
        if (orderCreatedEvent.Total <= 0)
        {
            throw new FinanceValidationException("Payment amount must be greater than zero.");
        }

        var existingPayment = await _paymentRepository.FindByOrderIdAsync(
            orderCreatedEvent.OrderId,
            cancellationToken);

        if (existingPayment is not null)
        {
            return PaymentMapper.ToResponse(existingPayment);
        }

        var now = DateTime.UtcNow;
        var payment = new Payment
        {
            Id = Guid.NewGuid(),
            OrderId = Required(orderCreatedEvent.OrderId, "OrderId"),
            CustomerAccountId = Required(orderCreatedEvent.CustomerAccountId, "CustomerAccountId"),
            RestaurantId = Required(orderCreatedEvent.RestaurantId, "RestaurantId"),
            BranchId = Required(orderCreatedEvent.BranchId, "BranchId"),
            Amount = orderCreatedEvent.Total,
            Currency = "MXN",
            Status = PaymentStatus.Pending,
            CreatedAt = now,
            UpdatedAt = now
        };

        try
        {
            var createdPayment = await _paymentRepository.CreateAsync(payment, cancellationToken);
            return PaymentMapper.ToResponse(createdPayment);
        }
        catch (InvalidOperationException)
        {
            var idempotentPayment = await _paymentRepository.FindByOrderIdAsync(
                orderCreatedEvent.OrderId,
                cancellationToken);

            if (idempotentPayment is not null)
            {
                return PaymentMapper.ToResponse(idempotentPayment);
            }

            throw;
        }
    }

    public async Task HandleOrderStatusChangedAsync(
        OrderStatusChangedEvent orderStatusChangedEvent,
        CancellationToken cancellationToken = default)
    {
        if (!string.Equals(orderStatusChangedEvent.NewStatus, CancelledOrderStatus, StringComparison.OrdinalIgnoreCase))
        {
            return;
        }

        var payment = await _paymentRepository.FindByOrderIdAsync(
            orderStatusChangedEvent.OrderId,
            cancellationToken);

        if (payment is null)
        {
            _logger.LogWarning(
                "Received cancelled order status event for order {OrderId}, but no payment exists.",
                orderStatusChangedEvent.OrderId);

            return;
        }

        if (payment.Status != PaymentStatus.Pending)
        {
            return;
        }

        await CancelAsync(payment.Id, cancellationToken);
    }

    public async Task<PaymentResponse> GetByIdAsync(
        Guid id,
        CancellationToken cancellationToken = default)
    {
        var payment = await GetPaymentOrThrowAsync(id, cancellationToken);

        return PaymentMapper.ToResponse(payment);
    }

    public async Task<PaymentResponse> GetByOrderIdAsync(
        string orderId,
        CancellationToken cancellationToken = default)
    {
        var payment = await _paymentRepository.FindByOrderIdAsync(orderId, cancellationToken);

        if (payment is null)
        {
            throw new PaymentNotFoundException(orderId);
        }

        return PaymentMapper.ToResponse(payment);
    }

    public async Task<IReadOnlyList<PaymentResponse>> GetByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        var payments = await _paymentRepository.FindByCustomerAccountIdAsync(
            customerAccountId,
            cancellationToken);

        return payments.Select(PaymentMapper.ToResponse).ToList();
    }

    public async Task<IReadOnlyList<PaymentResponse>> GetByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        var payments = await _paymentRepository.FindByRestaurantIdAsync(
            restaurantId,
            cancellationToken);

        return payments.Select(PaymentMapper.ToResponse).ToList();
    }

    public async Task<PaymentResponse> ApproveAsync(
        Guid id,
        ApprovePaymentRequest request,
        CancellationToken cancellationToken = default)
    {
        var payment = await GetPaymentOrThrowAsync(id, cancellationToken);

        EnsurePending(payment, PaymentStatus.Approved);

        var now = DateTime.UtcNow;
        payment.Status = PaymentStatus.Approved;
        payment.Provider = SimulatedProvider;
        payment.ProviderReference = string.IsNullOrWhiteSpace(request.ProviderReference)
            ? $"SIM-{Guid.NewGuid():N}"
            : request.ProviderReference;
        payment.ApprovedAt = now;
        payment.UpdatedAt = now;

        var updatedPayment = await _paymentRepository.UpdateAsync(payment, cancellationToken);

        return PaymentMapper.ToResponse(updatedPayment);
    }

    public async Task<PaymentResponse> RejectAsync(
        Guid id,
        RejectPaymentRequest request,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(request.RejectionReason))
        {
            throw new FinanceValidationException("Rejection reason is required.");
        }

        var payment = await GetPaymentOrThrowAsync(id, cancellationToken);

        EnsurePending(payment, PaymentStatus.Rejected);

        var now = DateTime.UtcNow;
        payment.Status = PaymentStatus.Rejected;
        payment.RejectionReason = request.RejectionReason.Trim();
        payment.RejectedAt = now;
        payment.UpdatedAt = now;

        var updatedPayment = await _paymentRepository.UpdateAsync(payment, cancellationToken);

        return PaymentMapper.ToResponse(updatedPayment);
    }

    public async Task<PaymentResponse> CancelAsync(
        Guid id,
        CancellationToken cancellationToken = default)
    {
        var payment = await GetPaymentOrThrowAsync(id, cancellationToken);

        EnsurePending(payment, PaymentStatus.Cancelled);

        var now = DateTime.UtcNow;
        payment.Status = PaymentStatus.Cancelled;
        payment.CancelledAt = now;
        payment.UpdatedAt = now;

        var updatedPayment = await _paymentRepository.UpdateAsync(payment, cancellationToken);

        return PaymentMapper.ToResponse(updatedPayment);
    }

    private async Task<Payment> GetPaymentOrThrowAsync(Guid id, CancellationToken cancellationToken)
    {
        var payment = await _paymentRepository.FindByIdAsync(id, cancellationToken);

        if (payment is null)
        {
            throw new PaymentNotFoundException(id.ToString());
        }

        return payment;
    }

    private static void EnsurePending(Payment payment, PaymentStatus requestedStatus)
    {
        if (payment.Status != PaymentStatus.Pending)
        {
            throw new InvalidPaymentStatusTransitionException(payment.Status, requestedStatus);
        }
    }

    private static string Required(string value, string fieldName)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new FinanceValidationException($"{fieldName} is required.");
        }

        return value;
    }
}
