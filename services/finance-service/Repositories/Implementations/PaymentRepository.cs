using FinanceService.Data;
using FinanceService.Domain.Entities;
using FinanceService.Repositories.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace FinanceService.Repositories.Implementations;

public sealed class PaymentRepository : IPaymentRepository
{
    private readonly FinanceDbContext _dbContext;

    public PaymentRepository(FinanceDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task<Payment> CreateAsync(
        Payment payment,
        CancellationToken cancellationToken = default)
    {
        _dbContext.Payments.Add(payment);
        await _dbContext.SaveChangesAsync(cancellationToken);

        return payment;
    }

    public Task<Payment?> FindByIdAsync(
        Guid id,
        CancellationToken cancellationToken = default)
    {
        return _dbContext.Payments
            .AsNoTracking()
            .FirstOrDefaultAsync(payment => payment.Id == id, cancellationToken);
    }

    public Task<Payment?> FindByOrderIdAsync(
        string orderId,
        CancellationToken cancellationToken = default)
    {
        return _dbContext.Payments
            .AsNoTracking()
            .FirstOrDefaultAsync(payment => payment.OrderId == orderId, cancellationToken);
    }

    public async Task<IReadOnlyList<Payment>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return await _dbContext.Payments
            .AsNoTracking()
            .Where(payment => payment.CustomerAccountId == customerAccountId)
            .OrderByDescending(payment => payment.CreatedAt)
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<Payment>> FindByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        return await _dbContext.Payments
            .AsNoTracking()
            .Where(payment => payment.RestaurantId == restaurantId)
            .OrderByDescending(payment => payment.CreatedAt)
            .ToListAsync(cancellationToken);
    }

    public async Task<Payment> UpdateAsync(
        Payment payment,
        CancellationToken cancellationToken = default)
    {
        _dbContext.Payments.Update(payment);
        await _dbContext.SaveChangesAsync(cancellationToken);

        return payment;
    }
}
