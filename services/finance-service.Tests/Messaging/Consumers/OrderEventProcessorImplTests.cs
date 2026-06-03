using System.Text.Json;
using FinanceService.Exceptions;
using FinanceService.Messaging.Consumers;
using FinanceService.Messaging.Events;
using FinanceService.Services.Interfaces;
using FinanceService.Tests.TestData;
using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;

namespace FinanceService.Tests.Messaging.Consumers;

public sealed class OrderEventProcessorImplTests
{
    private readonly Mock<IPaymentService> _paymentService = new();
    private readonly OrderEventProcessorImpl _processor;

    public OrderEventProcessorImplTests()
    {
        _processor = new OrderEventProcessorImpl(
            _paymentService.Object,
            NullLogger<OrderEventProcessorImpl>.Instance);
    }

    [Fact]
    public async Task ProcessesOrderCreatedAndCallsPaymentService()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();
        var rawMessage = JsonSerializer.Serialize(orderEvent);

        await _processor.ProcessAsync(rawMessage);

        _paymentService.Verify(
            service => service.CreatePendingPaymentFromOrderAsync(
                It.Is<OrderCreatedEvent>(message => message.OrderId == orderEvent.OrderId),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ProcessesOrderCreated_WithIncompleteEvent_PropagatesPaymentServiceValidationError()
    {
        var rawMessage = """
            {
              "EventType": "order.created",
              "OrderId": "",
              "CustomerAccountId": "customer-1",
              "RestaurantId": "restaurant-1",
              "BranchId": "branch-1",
              "Total": 150.75
            }
            """;
        _paymentService
            .Setup(service => service.CreatePendingPaymentFromOrderAsync(
                It.Is<OrderCreatedEvent>(message => message.OrderId == ""),
                It.IsAny<CancellationToken>()))
            .ThrowsAsync(new FinanceValidationException("OrderId is required."));

        var action = () => _processor.ProcessAsync(rawMessage);

        await action.Should().ThrowAsync<FinanceValidationException>();
    }

    [Fact]
    public async Task ProcessesOrderCreated_WhenPaymentServiceFails_PropagatesException()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();
        var rawMessage = JsonSerializer.Serialize(orderEvent);
        _paymentService
            .Setup(service => service.CreatePendingPaymentFromOrderAsync(
                It.IsAny<OrderCreatedEvent>(),
                It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("Unexpected processing failure."));

        var action = () => _processor.ProcessAsync(rawMessage);

        await action.Should().ThrowAsync<InvalidOperationException>()
            .WithMessage("Unexpected processing failure.");
    }

    [Fact]
    public async Task ProcessesOrderStatusChangedAndCallsPaymentService()
    {
        var orderEvent = PaymentTestData.OrderStatusChangedEvent();
        var rawMessage = JsonSerializer.Serialize(orderEvent);

        await _processor.ProcessAsync(rawMessage);

        _paymentService.Verify(
            service => service.HandleOrderStatusChangedAsync(
                It.Is<OrderStatusChangedEvent>(message => message.OrderId == orderEvent.OrderId),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task UnknownEventTypeDoesNotThrow()
    {
        var rawMessage = """
            {
              "EventType": "order.unknown",
              "OrderId": "order-1"
            }
            """;

        var action = () => _processor.ProcessAsync(rawMessage);

        await action.Should().NotThrowAsync();
        _paymentService.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task InvalidJsonDoesNotThrow()
    {
        var action = () => _processor.ProcessAsync("{ invalid-json");

        await action.Should().NotThrowAsync();
        _paymentService.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task MissingEventTypeDoesNotThrow()
    {
        var rawMessage = """
            {
              "OrderId": "order-1"
            }
            """;

        var action = () => _processor.ProcessAsync(rawMessage);

        await action.Should().NotThrowAsync();
        _paymentService.VerifyNoOtherCalls();
    }
}
