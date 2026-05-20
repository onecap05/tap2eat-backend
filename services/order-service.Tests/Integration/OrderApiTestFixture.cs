using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using MongoDB.Driver;
using OrderService.Domain.Documents;
using Testcontainers.MongoDb;

namespace OrderService.Tests.Integration;

public sealed class OrderApiTestFixture : IAsyncLifetime
{
    private const string TestDatabaseName = "tap2eat_orders_test";
    private const string OrdersCollectionName = "orders";

    private readonly MongoDbContainer _mongoDbContainer = new MongoDbBuilder()
        .WithImage("mongo:7.0")
        .Build();

    private IMongoCollection<OrderDocument>? _orders;

    public HttpClient Client { get; private set; } = null!;

    public IMongoCollection<OrderDocument> Orders =>
        _orders ?? throw new InvalidOperationException("MongoDB test collection is not initialized.");

    public string DatabaseName => TestDatabaseName;

    public async Task InitializeAsync()
    {
        await _mongoDbContainer.StartAsync();

        var factory = new OrderApiWebApplicationFactory(
            _mongoDbContainer.GetConnectionString(),
            TestDatabaseName,
            OrdersCollectionName);

        Client = factory.CreateClient();

        var mongoClient = new MongoClient(_mongoDbContainer.GetConnectionString());
        _orders = mongoClient
            .GetDatabase(TestDatabaseName)
            .GetCollection<OrderDocument>(OrdersCollectionName);
    }

    public async Task DisposeAsync()
    {
        Client.Dispose();
        await _mongoDbContainer.DisposeAsync();
    }

    private sealed class OrderApiWebApplicationFactory : WebApplicationFactory<Program>
    {
        private readonly string _connectionString;
        private readonly string _databaseName;
        private readonly string _ordersCollectionName;

        public OrderApiWebApplicationFactory(
            string connectionString,
            string databaseName,
            string ordersCollectionName)
        {
            _connectionString = connectionString;
            _databaseName = databaseName;
            _ordersCollectionName = ordersCollectionName;
        }

        protected override void ConfigureWebHost(IWebHostBuilder builder)
        {
            builder.ConfigureAppConfiguration((_, configurationBuilder) =>
            {
                configurationBuilder.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["MongoDb:ConnectionString"] = _connectionString,
                    ["MongoDb:DatabaseName"] = _databaseName,
                    ["MongoDb:OrdersCollectionName"] = _ordersCollectionName
                });
            });
        }
    }
}
