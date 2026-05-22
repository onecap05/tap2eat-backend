using FinanceService.Domain.Entities;
using FinanceService.Dtos.Responses;

namespace FinanceService.Mapping;

public static class PaymentMapper
{
    public static PaymentResponse ToResponse(Payment payment)
    {
        return new PaymentResponse
        {
            Id = payment.Id,
            OrderId = payment.OrderId,
            CustomerAccountId = payment.CustomerAccountId,
            RestaurantId = payment.RestaurantId,
            BranchId = payment.BranchId,
            Amount = payment.Amount,
            Currency = payment.Currency,
            Status = payment.Status,
            Provider = payment.Provider,
            ProviderReference = payment.ProviderReference,
            RejectionReason = payment.RejectionReason,
            CreatedAt = payment.CreatedAt,
            UpdatedAt = payment.UpdatedAt,
            ApprovedAt = payment.ApprovedAt,
            RejectedAt = payment.RejectedAt,
            CancelledAt = payment.CancelledAt
        };
    }
}
