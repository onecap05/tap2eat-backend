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

    public string OrderExchangeName { get; set; } = string.Empty;

    public string OrderQueueName { get; set; } = string.Empty;

    public string OrderRoutingKey { get; set; } = string.Empty;

    public string PaymentExchangeName { get; set; } = "tap2eat.payments";

    public string PaymentExchangeType { get; set; } = "topic";

    public bool Enabled { get; set; } = true;

    public string EffectiveOrderExchangeName =>
        string.IsNullOrWhiteSpace(OrderExchangeName) ? ExchangeName : OrderExchangeName;

    public string EffectiveOrderQueueName =>
        string.IsNullOrWhiteSpace(OrderQueueName) ? QueueName : OrderQueueName;

    public string EffectiveOrderRoutingKey =>
        string.IsNullOrWhiteSpace(OrderRoutingKey) ? RoutingKey : OrderRoutingKey;
}
