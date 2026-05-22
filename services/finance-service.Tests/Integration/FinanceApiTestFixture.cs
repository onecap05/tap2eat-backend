using FinanceService.Data;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Testcontainers.PostgreSql;

namespace FinanceService.Tests.Integration;

public sealed class FinanceApiTestFixture : IAsyncLifetime
{
    private readonly PostgreSqlContainer _postgresContainer = new PostgreSqlBuilder()
        .WithImage("postgres:16-alpine")
        .WithDatabase("tap2eat_finance_test")
        .WithUsername("test")
        .WithPassword("test")
        .Build();

    private FinanceApiWebApplicationFactory? _factory;

    public HttpClient Client { get; private set; } = null!;

    public IServiceProvider Services =>
        _factory?.Services ?? throw new InvalidOperationException("Factory is not initialized.");

    public async Task InitializeAsync()
    {
        await _postgresContainer.StartAsync();

        _factory = new FinanceApiWebApplicationFactory(_postgresContainer.GetConnectionString());
        Client = _factory.CreateClient();

        await ClearPaymentsAsync();
    }

    public async Task DisposeAsync()
    {
        Client.Dispose();
        _factory?.Dispose();

        await _postgresContainer.DisposeAsync();
    }

    public async Task ClearPaymentsAsync()
    {
        await using var scope = Services.CreateAsyncScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<FinanceDbContext>();

        await dbContext.Payments.ExecuteDeleteAsync();
    }

    private sealed class FinanceApiWebApplicationFactory : WebApplicationFactory<Program>
    {
        private readonly string _connectionString;

        public FinanceApiWebApplicationFactory(string connectionString)
        {
            _connectionString = connectionString;
        }

        protected override void ConfigureWebHost(IWebHostBuilder builder)
        {
            builder.ConfigureAppConfiguration((_, configurationBuilder) =>
            {
                configurationBuilder.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["Postgres:ConnectionString"] = _connectionString,
                    ["RabbitMq:Enabled"] = "false",
                    ["RabbitMq:HostName"] = "localhost",
                    ["RabbitMq:Port"] = "5672",
                    ["RabbitMq:UserName"] = "tap2eat",
                    ["RabbitMq:Password"] = "tap2eat",
                    ["RabbitMq:ExchangeName"] = "tap2eat.orders",
                    ["RabbitMq:QueueName"] = "tap2eat.finance.orders",
                    ["RabbitMq:RoutingKey"] = "order.#",
                    ["RabbitMq:OrderExchangeName"] = "tap2eat.orders",
                    ["RabbitMq:OrderQueueName"] = "tap2eat.finance.orders",
                    ["RabbitMq:OrderRoutingKey"] = "order.#",
                    ["RabbitMq:PaymentExchangeName"] = "tap2eat.payments",
                    ["RabbitMq:PaymentExchangeType"] = "topic"
                });
            });
        }
    }
}
