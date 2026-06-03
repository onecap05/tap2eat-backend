using System.Reflection;
using System.Text;
using FluentAssertions;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using Moq;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;
using RecommendationService.Config;
using RecommendationService.Messaging.Consumers;

namespace RecommendationService.Tests.Messaging;

public sealed class OrderEventConsumerBackgroundServiceTests
{
    [Fact]
    public async Task OnMessageReceivedAsync_WhenProcessorSucceeds_AcknowledgesMessage()
    {
        var processor = new Mock<IOrderDeliveredEventProcessor>();
        processor
            .Setup(item => item.ProcessRawAsync("valid-order-event", It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        var channel = new Mock<IChannel>();
        var service = CreateService(processor.Object, channel.Object);
        var eventArgs = CreateDeliverEventArgs("valid-order-event", deliveryTag: 20);

        await InvokeMessageReceivedAsync(service, eventArgs);

        processor.Verify(
            item => item.ProcessRawAsync("valid-order-event", It.IsAny<CancellationToken>()),
            Times.Once);
        channel.Verify(
            item => item.BasicAckAsync(20, false, It.IsAny<CancellationToken>()),
            Times.Once);
        channel.Verify(
            item => item.BasicNackAsync(
                It.IsAny<ulong>(),
                It.IsAny<bool>(),
                It.IsAny<bool>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task OnMessageReceivedAsync_WhenProcessorFails_RejectsMessageWithoutRequeue()
    {
        var processor = new Mock<IOrderDeliveredEventProcessor>();
        processor
            .Setup(item => item.ProcessRawAsync("broken-order-event", It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("Processor failed."));
        var channel = new Mock<IChannel>();
        var service = CreateService(processor.Object, channel.Object);
        var eventArgs = CreateDeliverEventArgs("broken-order-event", deliveryTag: 21);

        await InvokeMessageReceivedAsync(service, eventArgs);

        channel.Verify(
            item => item.BasicAckAsync(
                It.IsAny<ulong>(),
                It.IsAny<bool>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
        channel.Verify(
            item => item.BasicNackAsync(21, false, false, It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task OnMessageReceivedAsync_WhenChannelIsMissing_DoesNothing()
    {
        var processor = new Mock<IOrderDeliveredEventProcessor>();
        var service = CreateService(processor.Object, channel: null);
        var eventArgs = CreateDeliverEventArgs("valid-order-event", deliveryTag: 22);

        await InvokeMessageReceivedAsync(service, eventArgs);

        processor.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task StartAsync_WhenRabbitMqIsDisabled_CompletesWithoutCreatingScope()
    {
        var scopeFactory = new Mock<IServiceScopeFactory>();
        var service = new OrderEventConsumerBackgroundService(
            Options.Create(new RabbitMqSettings { Enabled = false }),
            scopeFactory.Object,
            NullLogger<OrderEventConsumerBackgroundService>.Instance);

        await service.StartAsync(CancellationToken.None);

        scopeFactory.Verify(item => item.CreateScope(), Times.Never);
    }

    [Fact]
    public async Task StartConsumerAsync_WhenRabbitMqIsUnavailable_ThrowsConnectionException()
    {
        var scopeFactory = new Mock<IServiceScopeFactory>();
        var service = new OrderEventConsumerBackgroundService(
            Options.Create(new RabbitMqSettings
            {
                Enabled = true,
                HostName = "127.0.0.1",
                Port = 1,
                UserName = "tap2eat",
                Password = "tap2eat"
            }),
            scopeFactory.Object,
            NullLogger<OrderEventConsumerBackgroundService>.Instance);

        var action = () => InvokeStartConsumerAsync(service, CancellationToken.None);

        await action.Should().ThrowAsync<Exception>();
        scopeFactory.Verify(item => item.CreateScope(), Times.Never);
    }

    [Fact]
    public async Task StopAsync_WhenChannelAndConnectionExist_ClosesAndDisposesBoth()
    {
        var processor = new Mock<IOrderDeliveredEventProcessor>();
        var channel = new Mock<IChannel>();
        var connection = new Mock<IConnection>();
        var service = CreateService(processor.Object, channel.Object);
        typeof(OrderEventConsumerBackgroundService)
            .GetField("_connection", BindingFlags.Instance | BindingFlags.NonPublic)!
            .SetValue(service, connection.Object);

        await service.StopAsync(CancellationToken.None);

        channel.Verify(
            item => item.CloseAsync(
                It.IsAny<ushort>(),
                It.IsAny<string>(),
                It.IsAny<bool>(),
                It.IsAny<CancellationToken>()),
            Times.Once);
        channel.Verify(item => item.DisposeAsync(), Times.Once);
        connection.Verify(
            item => item.CloseAsync(
                It.IsAny<ushort>(),
                It.IsAny<string>(),
                It.IsAny<TimeSpan>(),
                It.IsAny<bool>(),
                It.IsAny<CancellationToken>()),
            Times.Once);
        connection.Verify(item => item.DisposeAsync(), Times.Once);
    }

    private static OrderEventConsumerBackgroundService CreateService(
        IOrderDeliveredEventProcessor processor,
        IChannel? channel)
    {
        var scope = new Mock<IServiceScope>();
        var serviceProvider = new Mock<IServiceProvider>();
        serviceProvider
            .Setup(item => item.GetService(typeof(IOrderDeliveredEventProcessor)))
            .Returns(processor);
        scope
            .SetupGet(item => item.ServiceProvider)
            .Returns(serviceProvider.Object);

        var scopeFactory = new Mock<IServiceScopeFactory>();
        scopeFactory
            .Setup(item => item.CreateScope())
            .Returns(scope.Object);

        var service = new OrderEventConsumerBackgroundService(
            Options.Create(new RabbitMqSettings { Enabled = false }),
            scopeFactory.Object,
            NullLogger<OrderEventConsumerBackgroundService>.Instance);

        if (channel is not null)
        {
            typeof(OrderEventConsumerBackgroundService)
                .GetField("_channel", BindingFlags.Instance | BindingFlags.NonPublic)!
                .SetValue(service, channel);
        }

        return service;
    }

    private static BasicDeliverEventArgs CreateDeliverEventArgs(string body, ulong deliveryTag)
    {
        return new BasicDeliverEventArgs(
            consumerTag: "consumer-1",
            deliveryTag: deliveryTag,
            redelivered: false,
            exchange: "tap2eat.orders",
            routingKey: "order.status.changed",
            properties: new BasicProperties(),
            body: Encoding.UTF8.GetBytes(body));
    }

    private static async Task InvokeMessageReceivedAsync(
        OrderEventConsumerBackgroundService service,
        BasicDeliverEventArgs eventArgs)
    {
        var method = typeof(OrderEventConsumerBackgroundService)
            .GetMethod("OnMessageReceivedAsync", BindingFlags.Instance | BindingFlags.NonPublic)!;
        var task = (Task)method.Invoke(service, [service, eventArgs])!;

        await task;
    }

    private static async Task InvokeStartConsumerAsync(
        OrderEventConsumerBackgroundService service,
        CancellationToken cancellationToken)
    {
        var method = typeof(OrderEventConsumerBackgroundService)
            .GetMethod("StartConsumerAsync", BindingFlags.Instance | BindingFlags.NonPublic)!;
        var task = (Task)method.Invoke(service, [cancellationToken])!;

        await task;
    }
}
