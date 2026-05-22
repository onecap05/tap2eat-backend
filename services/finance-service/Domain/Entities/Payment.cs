using FinanceService.Domain.Enums;

namespace FinanceService.Domain.Entities;

public sealed class Payment
{
    public Guid Id { get; set; } = Guid.NewGuid();

    public string OrderId { get; set; } = string.Empty;

    public string CustomerAccountId { get; set; } = string.Empty;

    public string RestaurantId { get; set; } = string.Empty;

    public string BranchId { get; set; } = string.Empty;

    public decimal Amount { get; set; }

    public string Currency { get; set; } = "MXN";

    public PaymentStatus Status { get; set; } = PaymentStatus.Pending;

    public string? Provider { get; set; }

    public string? ProviderReference { get; set; }

    public string? RejectionReason { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? ApprovedAt { get; set; }

    public DateTime? RejectedAt { get; set; }

    public DateTime? CancelledAt { get; set; }
}
