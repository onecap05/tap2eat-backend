using OrderService.Domain.Documents;
using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
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

    public Task<OrderDocument?> FindByPublicTrackingCodeAsync(
        string publicTrackingCode,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult(_orders.FirstOrDefault(order => order.PublicTrackingCode == publicTrackingCode));
    }

    public Task<IReadOnlyList<OrderDocument>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return FindByCustomerAccountIdAsync(
            customerAccountId,
            new OrderQueryRequest(),
            cancellationToken);
    }

    public Task<IReadOnlyList<OrderDocument>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<OrderDocument>>(
            _orders
                .Where(order => order.CustomerAccountId == customerAccountId)
                .ApplyQuery(query));
    }

    public Task<IReadOnlyList<OrderDocument>> FindByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        return FindByRestaurantIdAsync(
            restaurantId,
            new OrderQueryRequest(),
            cancellationToken);
    }

    public Task<IReadOnlyList<OrderDocument>> FindByRestaurantIdAsync(
        string restaurantId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<OrderDocument>>(
            _orders
                .Where(order => order.RestaurantId == restaurantId)
                .ApplyQuery(query));
    }

    public Task<IReadOnlyList<OrderDocument>> FindByBranchIdAsync(
        string branchId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<OrderDocument>>(
            _orders
                .Where(order => order.BranchId == branchId)
                .ApplyQuery(query));
    }

    public Task<OrderDocument?> UpdateStatusAsync(
        string id,
        OrderStatus status,
        int? estimatedPreparationMinutes,
        DateTime? estimatedReadyAt,
        DateTime updatedAt,
        CancellationToken cancellationToken = default)
    {
        var order = _orders.FirstOrDefault(order => order.Id == id);

        if (order is null)
        {
            return Task.FromResult<OrderDocument?>(null);
        }

        order.Status = status;
        order.EstimatedPreparationMinutes = estimatedPreparationMinutes;
        order.EstimatedReadyAt = estimatedReadyAt;
        order.UpdatedAt = updatedAt;

        return Task.FromResult<OrderDocument?>(order);
    }
}

internal static class InMemoryOrderQueryExtensions
{
    public static List<OrderDocument> ApplyQuery(
        this IEnumerable<OrderDocument> orders,
        OrderQueryRequest query)
    {
        if (query.Status.HasValue)
        {
            orders = orders.Where(order => order.Status == query.Status.Value);
        }

        if (query.From.HasValue)
        {
            orders = orders.Where(order => order.CreatedAt >= query.From.Value);
        }

        if (query.To.HasValue)
        {
            orders = orders.Where(order => order.CreatedAt <= query.To.Value);
        }

        return orders
            .OrderByDescending(order => order.CreatedAt)
            .ToList();
    }
}
