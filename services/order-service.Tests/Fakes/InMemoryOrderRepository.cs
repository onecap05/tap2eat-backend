using OrderService.Domain.Documents;
using OrderService.Domain.Enums;
using OrderService.Repositories.Interfaces;

namespace OrderService.Tests.Fakes;

internal sealed class InMemoryOrderRepository : IOrderRepository
{
    private readonly List<OrderDocument> _orders = [];

    public InMemoryOrderRepository(params OrderDocument[] orders)
    {
        _orders.AddRange(orders);
    }

    public IReadOnlyList<OrderDocument> Orders => _orders;

    public Task<OrderDocument> CreateAsync(OrderDocument order, CancellationToken cancellationToken = default)
    {
        order.Id ??= MongoDB.Bson.ObjectId.GenerateNewId().ToString();
        _orders.Add(order);

        return Task.FromResult(order);
    }

    public Task<OrderDocument?> FindByIdAsync(string id, CancellationToken cancellationToken = default)
    {
        return Task.FromResult(_orders.FirstOrDefault(order => order.Id == id));
    }

    public Task<IReadOnlyList<OrderDocument>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<OrderDocument>>(
            _orders
                .Where(order => order.CustomerAccountId == customerAccountId)
                .OrderByDescending(order => order.CreatedAt)
                .ToList());
    }

    public Task<IReadOnlyList<OrderDocument>> FindByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<OrderDocument>>(
            _orders
                .Where(order => order.RestaurantId == restaurantId)
                .OrderByDescending(order => order.CreatedAt)
                .ToList());
    }

    public Task<OrderDocument?> UpdateStatusAsync(
        string id,
        OrderStatus status,
        DateTime updatedAt,
        CancellationToken cancellationToken = default)
    {
        var order = _orders.FirstOrDefault(order => order.Id == id);

        if (order is null)
        {
            return Task.FromResult<OrderDocument?>(null);
        }

        order.Status = status;
        order.UpdatedAt = updatedAt;

        return Task.FromResult<OrderDocument?>(order);
    }
}
