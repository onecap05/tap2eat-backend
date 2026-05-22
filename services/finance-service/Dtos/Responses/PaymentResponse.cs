using FinanceService.Domain.Enums;

namespace FinanceService.Dtos.Responses;

public sealed class PaymentResponse
{
    public Guid Id { get; set; }

    public string OrderId { get; set; } = string.Empty;

    public string CustomerAccountId { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public decimal Amount { get; set; }

    public string Currency { get; set; } = string.Empty;

    public PaymentStatus Status { get; set; }

    public string? Provider { get; set; }

    public string? ProviderReference { get; set; }

    public string? RejectionReason { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }

    public DateTime? ApprovedAt { get; set; }

    public DateTime? RejectedAt { get; set; }

    public DateTime? CancelledAt { get; set; }
}
