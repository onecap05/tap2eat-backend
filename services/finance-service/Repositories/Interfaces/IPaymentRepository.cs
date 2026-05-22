using FinanceService.Domain.Entities;

namespace FinanceService.Repositories.Interfaces;

public interface IPaymentRepository
{
    Task<Payment> CreateAsync(Payment payment, CancellationToken cancellationToken = default);

    Task<Payment?> FindByIdAsync(Guid id, CancellationToken cancellationToken = default);

    Task<Payment?> FindByOrderIdAsync(string orderId, CancellationToken cancellationToken = default);

    Task<IReadOnlyList<Payment>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<Payment>> FindByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<Payment> UpdateAsync(Payment payment, CancellationToken cancellationToken = default);
}
