using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using OrderService.Config;
using OrderService.Domain.Documents;
using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
using OrderService.Repositories.Interfaces;

namespace OrderService.Repositories.Implementations;

public sealed class OrderRepository : IOrderRepository
{
    private readonly IMongoCollection<OrderDocument> _orders;

    public OrderRepository(IMongoClient mongoClient, IOptions<MongoDbSettings> options)
    {
        var settings = options.Value;
        var database = mongoClient.GetDatabase(settings.DatabaseName);

        _orders = database.GetCollection<OrderDocument>(settings.OrdersCollectionName);
    }

    public async Task<OrderDocument> CreateAsync(
        OrderDocument order,
        CancellationToken cancellationToken = default)
    {
        await _orders.InsertOneAsync(order, cancellationToken: cancellationToken);
        return order;
    }

    public async Task<OrderDocument?> FindByIdAsync(
        string id,
        CancellationToken cancellationToken = default)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            return null;
        }

        return await _orders
            .Find(order => order.Id == id)
            .FirstOrDefaultAsync(cancellationToken);
    }

    public async Task<OrderDocument?> FindByPublicTrackingCodeAsync(
        string publicTrackingCode,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(publicTrackingCode))
        {
            return null;
        }

        return await _orders
            .Find(order => order.PublicTrackingCode == publicTrackingCode)
            .FirstOrDefaultAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<OrderDocument>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return await FindByCustomerAccountIdAsync(
            customerAccountId,
            new OrderQueryRequest(),
            cancellationToken);
    }

    public async Task<IReadOnlyList<OrderDocument>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        return await _orders
            .Find(BuildQueryFilter(
                Builders<OrderDocument>.Filter.Eq(order => order.CustomerAccountId, customerAccountId),
                query))
            .SortByDescending(order => order.CreatedAt)
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<OrderDocument>> FindByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        return await FindByRestaurantIdAsync(
            restaurantId,
            new OrderQueryRequest(),
            cancellationToken);
    }

    public async Task<IReadOnlyList<OrderDocument>> FindByRestaurantIdAsync(
        string restaurantId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        return await _orders
            .Find(BuildQueryFilter(
                Builders<OrderDocument>.Filter.Eq(order => order.RestaurantId, restaurantId),
                query))
            .SortByDescending(order => order.CreatedAt)
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<OrderDocument>> FindByBranchIdAsync(
        string branchId,
        OrderQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        return await _orders
            .Find(BuildQueryFilter(
                Builders<OrderDocument>.Filter.Eq(order => order.BranchId, branchId),
                query))
            .SortByDescending(order => order.CreatedAt)
            .ToListAsync(cancellationToken);
    }

    public async Task<OrderDocument?> UpdateStatusAsync(
        string id,
        OrderStatus status,
        DateTime updatedAt,
        CancellationToken cancellationToken = default)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            return null;
        }

        var update = Builders<OrderDocument>.Update
            .Set(order => order.Status, status)
            .Set(order => order.UpdatedAt, updatedAt);

        return await _orders.FindOneAndUpdateAsync(
            order => order.Id == id,
            update,
            new FindOneAndUpdateOptions<OrderDocument>
            {
                ReturnDocument = ReturnDocument.After
            },
            cancellationToken);
    }

    private static FilterDefinition<OrderDocument> BuildQueryFilter(
        FilterDefinition<OrderDocument> baseFilter,
        OrderQueryRequest query)
    {
        var filters = new List<FilterDefinition<OrderDocument>> { baseFilter };
        var builder = Builders<OrderDocument>.Filter;

        if (query.Status.HasValue)
        {
            filters.Add(builder.Eq(order => order.Status, query.Status.Value));
        }

        if (query.From.HasValue)
        {
            filters.Add(builder.Gte(order => order.CreatedAt, query.From.Value));
        }

        if (query.To.HasValue)
        {
            filters.Add(builder.Lte(order => order.CreatedAt, query.To.Value));
        }

        return builder.And(filters);
    }
}
