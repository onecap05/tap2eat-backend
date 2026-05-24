using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using MongoDB.Driver;
using OrderService.Domain.Documents;
using OrderService.Messaging.Publishers;
using OrderService.Tests.Fakes;
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
    private OrderApiWebApplicationFactory? _factory;

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

        _factory = new OrderApiWebApplicationFactory(
            _mongoDbContainer.GetConnectionString(),
            TestDatabaseName,
            OrdersCollectionName,
            _catalogStubServer.BaseUrl);

        Client = _factory.CreateClient();

        var mongoClient = new MongoClient(_mongoDbContainer.GetConnectionString());

        _orders = mongoClient
            .GetDatabase(TestDatabaseName)
            .GetCollection<OrderDocument>(OrdersCollectionName);
    }

    public async Task DisposeAsync()
    {
        Client.Dispose();
        _factory?.Dispose();

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
                    ["CatalogService:InternalServiceToken"] = "test-internal-token",

                    ["RabbitMq:Enabled"] = "false",
                    ["RabbitMq:HostName"] = "localhost",
                    ["RabbitMq:Port"] = "5672",
                    ["RabbitMq:UserName"] = "tap2eat",
                    ["RabbitMq:Password"] = "tap2eat",
                    ["RabbitMq:ExchangeName"] = "tap2eat.orders",
                    ["RabbitMq:ExchangeType"] = "topic"
                });
            });

            builder.ConfigureServices(services =>
            {
                services.AddAuthentication(options =>
                    {
                        options.DefaultAuthenticateScheme = TestAuthHandler.SchemeName;
                        options.DefaultChallengeScheme = TestAuthHandler.SchemeName;
                    })
                    .AddScheme<AuthenticationSchemeOptions, TestAuthHandler>(
                        TestAuthHandler.SchemeName,
                        _ => { });

                services.RemoveAll<IOrderEventPublisher>();
                services.AddSingleton<IOrderEventPublisher, FakeOrderEventPublisher>();
            });
        }
    }
}
