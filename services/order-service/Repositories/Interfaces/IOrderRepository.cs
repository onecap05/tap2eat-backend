using OrderService.Domain.Documents;
using OrderService.Domain.Enums;

namespace OrderService.Repositories.Interfaces;

public interface IOrderRepository
{
    Task<OrderDocument> CreateAsync(OrderDocument order, CancellationToken cancellationToken = default);

    Task<OrderDocument?> FindByIdAsync(string id, CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderDocument>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<OrderDocument>> FindByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default);

    Task<OrderDocument?> UpdateStatusAsync(
        string id,
        OrderStatus status,
        DateTime updatedAt,
        CancellationToken cancellationToken = default);
}
