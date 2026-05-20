using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using OrderService.Config;
using OrderService.Domain.Documents;
using OrderService.Domain.Enums;
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

    public async Task<IReadOnlyList<OrderDocument>> FindByCustomerAccountIdAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return await _orders
            .Find(order => order.CustomerAccountId == customerAccountId)
            .SortByDescending(order => order.CreatedAt)
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<OrderDocument>> FindByRestaurantIdAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        return await _orders
            .Find(order => order.RestaurantId == restaurantId)
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
}
