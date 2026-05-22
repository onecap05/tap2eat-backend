using System.Text;
using System.Text.Json;
using FinanceService.Config;
using FinanceService.Domain.Entities;
using FinanceService.Messaging.Events;
using Microsoft.Extensions.Options;
using RabbitMQ.Client;

namespace FinanceService.Messaging.Publishers;

public sealed class RabbitMqPaymentEventPublisherImpl : IPaymentEventPublisher
{
    private const string PaymentApprovedRoutingKey = "payment.approved";
    private const string PaymentRejectedRoutingKey = "payment.rejected";
    private const string PaymentCancelledRoutingKey = "payment.cancelled";

    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly RabbitMqSettings _settings;
    private readonly ILogger<RabbitMqPaymentEventPublisherImpl> _logger;

    public RabbitMqPaymentEventPublisherImpl(
        IOptions<RabbitMqSettings> options,
        ILogger<RabbitMqPaymentEventPublisherImpl> logger)
    {
        _settings = options.Value;
        _logger = logger;
    }

    public Task PublishPaymentApprovedAsync(
        Payment payment,
        CancellationToken cancellationToken = default)
    {
        var paymentEvent = new PaymentApprovedEvent
        {
            PaymentId = payment.Id,
            OrderId = payment.OrderId,
            CustomerAccountId = payment.CustomerAccountId,
            RestaurantId = payment.RestaurantId,
            BranchId = payment.BranchId,
            Amount = payment.Amount,
            Currency = payment.Currency,
            Status = payment.Status.ToString(),
            Provider = payment.Provider,
            ProviderReference = payment.ProviderReference
        };

        return PublishAsync(PaymentApprovedRoutingKey, paymentEvent, cancellationToken);
    }

    public Task PublishPaymentRejectedAsync(
        Payment payment,
        CancellationToken cancellationToken = default)
    {
        var paymentEvent = new PaymentRejectedEvent
        {
            PaymentId = payment.Id,
            OrderId = payment.OrderId,
            CustomerAccountId = payment.CustomerAccountId,
            RestaurantId = payment.RestaurantId,
            BranchId = payment.BranchId,
            Amount = payment.Amount,
            Currency = payment.Currency,
            Status = payment.Status.ToString(),
            RejectionReason = payment.RejectionReason
        };

        return PublishAsync(PaymentRejectedRoutingKey, paymentEvent, cancellationToken);
    }

    public Task PublishPaymentCancelledAsync(
        Payment payment,
        string? reason = null,
        CancellationToken cancellationToken = default)
    {
        var paymentEvent = new PaymentCancelledEvent
        {
            PaymentId = payment.Id,
            OrderId = payment.OrderId,
            CustomerAccountId = payment.CustomerAccountId,
            RestaurantId = payment.RestaurantId,
            BranchId = payment.BranchId,
            Amount = payment.Amount,
            Currency = payment.Currency,
            Status = payment.Status.ToString(),
            Reason = reason
        };

        return PublishAsync(PaymentCancelledRoutingKey, paymentEvent, cancellationToken);
    }

    private async Task PublishAsync<TEvent>(
        string routingKey,
        TEvent paymentEvent,
        CancellationToken cancellationToken)
    {
        if (!_settings.Enabled)
        {
            _logger.LogInformation(
                "RabbitMQ payment publisher is disabled. Skipping {RoutingKey} event.",
                routingKey);
            return;
        }

        try
        {
            var factory = new ConnectionFactory
            {
                HostName = _settings.HostName,
                Port = _settings.Port,
                UserName = _settings.UserName,
                Password = _settings.Password
            };

            await using var connection = await factory.CreateConnectionAsync(cancellationToken);
            await using var channel = await connection.CreateChannelAsync(cancellationToken: cancellationToken);

            await channel.ExchangeDeclareAsync(
                exchange: _settings.PaymentExchangeName,
                type: _settings.PaymentExchangeType,
                durable: true,
                autoDelete: false,
                arguments: null,
                cancellationToken: cancellationToken);

            var body = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(paymentEvent, JsonOptions));
            var properties = new BasicProperties
            {
                ContentType = "application/json",
                DeliveryMode = DeliveryModes.Persistent
            };

            await channel.BasicPublishAsync(
                exchange: _settings.PaymentExchangeName,
                routingKey: routingKey,
                mandatory: false,
                basicProperties: properties,
                body: body,
                cancellationToken: cancellationToken);

            _logger.LogInformation(
                "Published payment event {RoutingKey} to exchange {ExchangeName}.",
                routingKey,
                _settings.PaymentExchangeName);
        }
        catch (Exception exception)
        {
            _logger.LogError(
                exception,
                "Failed to publish payment event {RoutingKey}.",
                routingKey);
        }
    }
}
