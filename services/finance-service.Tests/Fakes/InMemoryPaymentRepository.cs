using FinanceService.Domain.Entities;
using FinanceService.Repositories.Interfaces;

namespace FinanceService.Tests.Fakes;

public sealed class InMemoryPaymentRepository : IPaymentRepository
{
    private readonly List<Payment> _payments = [];

    public Task<Payment> CreateAsync(
        Payment payment,
        CancellationToken cancellationToken = default)
    {
        if (_payments.Any(existing => existing.OrderId == payment.OrderId))
        {
            throw new InvalidOperationException("A payment with the same OrderId already exists.");
        }

        _payments.Add(Clone(payment));

        return Task.FromResult(Clone(payment));
    }

    public Task<Payment?> FindByIdAsync(
        Guid id,
        CancellationToken cancellationToken = default)
    {
        var payment = _payments.FirstOrDefault(existing => existing.Id == id);

        return Task.FromResult(payment is null ? null : Clone(payment));
    }

    public Task<Payment?> FindByOrderIdAsync(
        string orderId,
        CancellationToken cancellationToken = default)
    {
        var payment = _payments.FirstOrDefault(existing => existing.OrderId == orderId);

        return Task.FromResult(payment is null ? null : Clone(payment));
    }

    public Task<IReadOnlyList<Payment>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        IReadOnlyList<Payment> payments = _payments
            .Where(payment => payment.CustomerAccountId == customerAccountId)
            .OrderByDescending(payment => payment.CreatedAt)
            .Select(Clone)
            .ToList();

        return Task.FromResult(payments);
    }

    public Task<IReadOnlyList<Payment>> FindByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        IReadOnlyList<Payment> payments = _payments
            .Where(payment => payment.RestaurantId == restaurantId)
            .OrderByDescending(payment => payment.CreatedAt)
            .Select(Clone)
            .ToList();

        return Task.FromResult(payments);
    }

    public Task<Payment> UpdateAsync(
        Payment payment,
        CancellationToken cancellationToken = default)
    {
        var index = _payments.FindIndex(existing => existing.Id == payment.Id);

        if (index < 0)
        {
            throw new InvalidOperationException("Payment does not exist.");
        }

        _payments[index] = Clone(payment);

        return Task.FromResult(Clone(payment));
    }

    public void Seed(Payment payment)
    {
        _payments.Add(Clone(payment));
    }

    private static Payment Clone(Payment payment)
    {
        return new Payment
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
            AmountReceived = payment.AmountReceived,
            ChangeAmount = payment.ChangeAmount,
            RejectionReason = payment.RejectionReason,
            CreatedAt = payment.CreatedAt,
            UpdatedAt = payment.UpdatedAt,
            ApprovedAt = payment.ApprovedAt,
            RejectedAt = payment.RejectedAt,
            CancelledAt = payment.CancelledAt
        };
    }
}
