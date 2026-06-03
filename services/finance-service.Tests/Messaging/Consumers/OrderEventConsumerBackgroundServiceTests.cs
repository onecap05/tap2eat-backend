using System.Reflection;
using System.Text;
using FinanceService.Config;
using FinanceService.Messaging.Consumers;
using FluentAssertions;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using Moq;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace FinanceService.Tests.Messaging.Consumers;

public sealed class OrderEventConsumerBackgroundServiceTests
{
    [Fact]
    public async Task OnMessageReceivedAsync_WhenProcessorSucceeds_AcknowledgesMessage()
    {
        var processor = new Mock<IOrderEventProcessor>();
        processor
            .Setup(service => service.ProcessAsync("valid-order-event", It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        var channel = new Mock<IChannel>();
        var service = CreateService(processor.Object, channel.Object);
        var eventArgs = CreateDeliverEventArgs("valid-order-event", deliveryTag: 10);

        await InvokeMessageReceivedAsync(service, eventArgs);

        processor.Verify(
            service => service.ProcessAsync("valid-order-event", It.IsAny<CancellationToken>()),
            Times.Once);
        channel.Verify(
            channel => channel.BasicAckAsync(10, false, It.IsAny<CancellationToken>()),
            Times.Once);
        channel.Verify(
            channel => channel.BasicNackAsync(
                It.IsAny<ulong>(),
                It.IsAny<bool>(),
                It.IsAny<bool>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task OnMessageReceivedAsync_WhenProcessorFails_RejectsMessageWithoutRequeue()
    {
        var processor = new Mock<IOrderEventProcessor>();
        processor
            .Setup(service => service.ProcessAsync("broken-order-event", It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("Processor failed."));
        var channel = new Mock<IChannel>();
        var service = CreateService(processor.Object, channel.Object);
        var eventArgs = CreateDeliverEventArgs("broken-order-event", deliveryTag: 11);

        await InvokeMessageReceivedAsync(service, eventArgs);

        channel.Verify(
            channel => channel.BasicAckAsync(
                It.IsAny<ulong>(),
                It.IsAny<bool>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
        channel.Verify(
            channel => channel.BasicNackAsync(11, false, false, It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task OnMessageReceivedAsync_WhenChannelIsMissing_DoesNothing()
    {
        var processor = new Mock<IOrderEventProcessor>();
        var service = CreateService(processor.Object, channel: null);
        var eventArgs = CreateDeliverEventArgs("valid-order-event", deliveryTag: 12);

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

        scopeFactory.Verify(factory => factory.CreateScope(), Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_WhenRabbitMqIsEnabledButUnavailable_ThrowsConnectionException()
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

        var action = () => InvokeExecuteAsync(service, CancellationToken.None);

        await action.Should().ThrowAsync<Exception>();
        scopeFactory.Verify(factory => factory.CreateScope(), Times.Never);
    }

    [Fact]
    public async Task StopAsync_WhenChannelAndConnectionExist_ClosesAndDisposesBoth()
    {
        var processor = new Mock<IOrderEventProcessor>();
        var channel = new Mock<IChannel>();
        var connection = new Mock<IConnection>();
        var service = CreateService(processor.Object, channel.Object);
        typeof(OrderEventConsumerBackgroundService)
            .GetField("_connection", BindingFlags.Instance | BindingFlags.NonPublic)!
            .SetValue(service, connection.Object);

        await service.StopAsync(CancellationToken.None);

        channel.Verify(
            channel => channel.CloseAsync(
                It.IsAny<ushort>(),
                It.IsAny<string>(),
                It.IsAny<bool>(),
                It.IsAny<CancellationToken>()),
            Times.Once);
        channel.Verify(channel => channel.DisposeAsync(), Times.Once);
        connection.Verify(
            connection => connection.CloseAsync(
                It.IsAny<ushort>(),
                It.IsAny<string>(),
                It.IsAny<TimeSpan>(),
                It.IsAny<bool>(),
                It.IsAny<CancellationToken>()),
            Times.Once);
        connection.Verify(connection => connection.DisposeAsync(), Times.Once);
    }

    private static OrderEventConsumerBackgroundService CreateService(
        IOrderEventProcessor processor,
        IChannel? channel)
    {
        var scope = new Mock<IServiceScope>();
        var serviceProvider = new Mock<IServiceProvider>();
        serviceProvider
            .Setup(provider => provider.GetService(typeof(IOrderEventProcessor)))
            .Returns(processor);
        scope
            .SetupGet(scope => scope.ServiceProvider)
            .Returns(serviceProvider.Object);

        var scopeFactory = new Mock<IServiceScopeFactory>();
        scopeFactory
            .Setup(factory => factory.CreateScope())
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
            routingKey: "order.created",
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

    private static async Task InvokeExecuteAsync(
        OrderEventConsumerBackgroundService service,
        CancellationToken cancellationToken)
    {
        var method = typeof(OrderEventConsumerBackgroundService)
            .GetMethod("ExecuteAsync", BindingFlags.Instance | BindingFlags.NonPublic)!;

        var task = (Task)method.Invoke(service, [cancellationToken])!;

        await task;
    }
}
