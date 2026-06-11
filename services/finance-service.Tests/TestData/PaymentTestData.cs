using FinanceService.Domain.Entities;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Messaging.Events;

namespace FinanceService.Tests.TestData;

public static class PaymentTestData
{
    public static OrderCreatedEvent OrderCreatedEvent(
        string orderId = "order-1",
        decimal total = 150.75m)
    {
        return new OrderCreatedEvent
        {
            EventId = Guid.NewGuid(),
            EventType = "order.created",
            OrderId = orderId,
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            Subtotal = total - 10m,
            Total = total,
            Status = "Created",
            CreatedAt = DateTime.UtcNow.AddMinutes(-1),
            OccurredAt = DateTime.UtcNow
        };
    }

    public static OrderStatusChangedEvent OrderStatusChangedEvent(
        string orderId = "order-1",
        string newStatus = "Cancelled")
    {
        return new OrderStatusChangedEvent
        {
            EventId = Guid.NewGuid(),
            EventType = "order.status.changed",
            OrderId = orderId,
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            PreviousStatus = "Created",
            NewStatus = newStatus,
            OccurredAt = DateTime.UtcNow
        };
    }

    public static Payment Payment(
        Guid? id = null,
        string orderId = "order-1",
        PaymentStatus status = PaymentStatus.Pending)
    {
        var now = DateTime.UtcNow;

        return new Payment
        {
            Id = id ?? Guid.NewGuid(),
            OrderId = orderId,
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            Amount = 150.75m,
            Currency = "MXN",
            Status = status,
            CreatedAt = now,
            UpdatedAt = now
        };
    }

    public static ApprovePaymentRequest ApproveRequest(string? providerReference = "manual-ref-1")
    {
        return new ApprovePaymentRequest
        {
            ProviderReference = providerReference
        };
    }

    public static ConfirmCashPaymentRequest ConfirmCashRequest(decimal amountReceived = 200m)
    {
        return new ConfirmCashPaymentRequest
        {
            AmountReceived = amountReceived
        };
    }

    public static RejectPaymentRequest RejectRequest(string reason = "Insufficient funds")
    {
        return new RejectPaymentRequest
        {
            RejectionReason = reason
        };
    }
}
