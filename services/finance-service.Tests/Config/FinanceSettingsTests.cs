using FinanceService.Config;
using FluentAssertions;

namespace FinanceService.Tests.Config;

public sealed class FinanceSettingsTests
{
    [Fact]
    public void JwtSettings_DefaultsToGatewayPublicKeyPath()
    {
        var settings = new JwtSettings();

        JwtSettings.SectionName.Should().Be("Jwt");
        settings.PublicKeyPath.Should().Be("../api-gateway/src/main/resources/keys/public_key.pem");
    }

    [Fact]
    public void PostgresSettings_DefaultsToEmptyConnectionString()
    {
        var settings = new PostgresSettings();

        PostgresSettings.SectionName.Should().Be("Postgres");
        settings.ConnectionString.Should().BeEmpty();
    }

    [Fact]
    public void RabbitMqSettings_UsesLegacyOrderValuesWhenSpecificOrderValuesAreEmpty()
    {
        var settings = new RabbitMqSettings
        {
            ExchangeName = "legacy.exchange",
            QueueName = "legacy.queue",
            RoutingKey = "legacy.routing",
            OrderExchangeName = "",
            OrderQueueName = " ",
            OrderRoutingKey = ""
        };

        RabbitMqSettings.SectionName.Should().Be("RabbitMq");
        settings.EffectiveOrderExchangeName.Should().Be("legacy.exchange");
        settings.EffectiveOrderQueueName.Should().Be("legacy.queue");
        settings.EffectiveOrderRoutingKey.Should().Be("legacy.routing");
    }

    [Fact]
    public void RabbitMqSettings_UsesSpecificOrderValuesWhenConfigured()
    {
        var settings = new RabbitMqSettings
        {
            ExchangeName = "legacy.exchange",
            QueueName = "legacy.queue",
            RoutingKey = "legacy.routing",
            OrderExchangeName = "orders.exchange",
            OrderQueueName = "orders.queue",
            OrderRoutingKey = "order.created"
        };

        settings.EffectiveOrderExchangeName.Should().Be("orders.exchange");
        settings.EffectiveOrderQueueName.Should().Be("orders.queue");
        settings.EffectiveOrderRoutingKey.Should().Be("order.created");
    }
}
