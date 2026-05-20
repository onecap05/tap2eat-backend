using OrderService.Domain.Documents;

namespace OrderService.Repositories.Interfaces;

public interface IOrderRepository
{
    Task<OrderDocument> CreateAsync(OrderDocument order, CancellationToken cancellationToken = default);

    Task<OrderDocument?> FindByIdAsync(string id, CancellationToken cancellationToken = default);
}