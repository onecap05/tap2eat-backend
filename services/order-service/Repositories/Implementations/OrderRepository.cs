using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using OrderService.Config;
using OrderService.Domain.Documents;
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
}