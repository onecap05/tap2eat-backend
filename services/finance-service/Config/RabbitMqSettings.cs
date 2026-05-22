namespace FinanceService.Config;

public sealed class RabbitMqSettings
{
    public const string SectionName = "RabbitMq";

    public string HostName { get; set; } = "localhost";

    public int Port { get; set; } = 5672;

    public string UserName { get; set; } = "tap2eat";

    public string Password { get; set; } = "tap2eat";

    public string ExchangeName { get; set; } = "tap2eat.orders";

    public string QueueName { get; set; } = "tap2eat.finance.orders";

    public string RoutingKey { get; set; } = "order.#";

    public bool Enabled { get; set; } = true;
}
