using FinanceService.Config;
using FinanceService.Messaging.Publishers;
using FinanceService.Tests.TestData;
using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;

namespace FinanceService.Tests.Messaging.Publishers;

public sealed class RabbitMqPaymentEventPublisherImplTests
{
    private readonly RabbitMqPaymentEventPublisherImpl _publisher = new(
        Options.Create(new RabbitMqSettings { Enabled = false }),
        NullLogger<RabbitMqPaymentEventPublisherImpl>.Instance);

    [Fact]
    public async Task PublishPaymentApprovedAsync_WhenRabbitMqDisabled_ShouldNotThrow()
    {
        var action = () => _publisher.PublishPaymentApprovedAsync(PaymentTestData.Payment());

        await action.Should().NotThrowAsync();
    }

    [Fact]
    public async Task PublishPaymentRejectedAsync_WhenRabbitMqDisabled_ShouldNotThrow()
    {
        var payment = PaymentTestData.Payment();
        payment.RejectionReason = "Declined";

        var action = () => _publisher.PublishPaymentRejectedAsync(payment);

        await action.Should().NotThrowAsync();
    }

    [Fact]
    public async Task PublishPaymentCancelledAsync_WhenRabbitMqDisabled_ShouldNotThrow()
    {
        var action = () => _publisher.PublishPaymentCancelledAsync(
            PaymentTestData.Payment(),
            "ORDER_CANCELLED");

        await action.Should().NotThrowAsync();
    }
}
