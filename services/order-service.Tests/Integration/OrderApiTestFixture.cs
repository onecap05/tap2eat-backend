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
    private CatalogStubServer? _catalogStubServer;

    public HttpClient Client { get; private set; } = null!;

    public IMongoCollection<OrderDocument> Orders =>
        _orders ?? throw new InvalidOperationException("MongoDB test collection is not initialized.");

    public string DatabaseName => TestDatabaseName;

    public CatalogStubServer Catalog =>
        _catalogStubServer ?? throw new InvalidOperationException("Catalog stub server is not initialized.");

    public async Task InitializeAsync()
    {
        await _mongoDbContainer.StartAsync();
        _catalogStubServer = new CatalogStubServer();
        await _catalogStubServer.StartAsync();

        var factory = new OrderApiWebApplicationFactory(
            _mongoDbContainer.GetConnectionString(),
            TestDatabaseName,
            OrdersCollectionName,
            _catalogStubServer.BaseUrl);

        Client = factory.CreateClient();

        var mongoClient = new MongoClient(_mongoDbContainer.GetConnectionString());
        _orders = mongoClient
            .GetDatabase(TestDatabaseName)
            .GetCollection<OrderDocument>(OrdersCollectionName);
    }

    public async Task DisposeAsync()
    {
        Client.Dispose();
        if (_catalogStubServer is not null)
        {
            await _catalogStubServer.DisposeAsync();
        }
        await _mongoDbContainer.DisposeAsync();
    }

    private sealed class OrderApiWebApplicationFactory : WebApplicationFactory<Program>
    {
        private readonly string _connectionString;
        private readonly string _databaseName;
        private readonly string _ordersCollectionName;
        private readonly string _catalogBaseUrl;

        public OrderApiWebApplicationFactory(
            string connectionString,
            string databaseName,
            string ordersCollectionName,
            string catalogBaseUrl)
        {
            _connectionString = connectionString;
            _databaseName = databaseName;
            _ordersCollectionName = ordersCollectionName;
            _catalogBaseUrl = catalogBaseUrl;
        }

        protected override void ConfigureWebHost(IWebHostBuilder builder)
        {
            builder.ConfigureAppConfiguration((_, configurationBuilder) =>
            {
                configurationBuilder.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["MongoDb:ConnectionString"] = _connectionString,
                    ["MongoDb:DatabaseName"] = _databaseName,
                    ["MongoDb:OrdersCollectionName"] = _ordersCollectionName,
                    ["CatalogService:BaseUrl"] = _catalogBaseUrl,
                    ["CatalogService:InternalServiceToken"] = "test-internal-token"
                });
            });
        }
    }
}
