using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Exceptions;
using FinanceService.Messaging.Publishers;
using FinanceService.Repositories.Interfaces;
using FinanceService.Services.Implementations;
using FinanceService.Tests.Fakes;
using FinanceService.Tests.TestData;
using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;

namespace FinanceService.Tests.Services;

public sealed class PaymentServiceImplTests
{
    private readonly InMemoryPaymentRepository _repository = new();
    private readonly FakePaymentEventPublisher _publisher = new();
    private readonly PaymentServiceImpl _service;

    public PaymentServiceImplTests()
    {
        _service = new PaymentServiceImpl(
            _repository,
            _publisher,
            NullLogger<PaymentServiceImpl>.Instance);
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_createsPendingPayment()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();

        var payment = await _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        payment.OrderId.Should().Be(orderEvent.OrderId);
        payment.Amount.Should().Be(orderEvent.Total);
        payment.Status.Should().Be(PaymentStatus.Pending);
        payment.Currency.Should().Be("MXN");
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_isIdempotentForSameOrderId()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();

        var firstPayment = await _service.CreatePendingPaymentFromOrderAsync(orderEvent);
        var secondPayment = await _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        secondPayment.Id.Should().Be(firstPayment.Id);
        secondPayment.OrderId.Should().Be(firstPayment.OrderId);
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_WhenPaymentAlreadyExists_ShouldReturnExistingAndNotDuplicate()
    {
        var existingPayment = PaymentTestData.Payment(orderId: "existing-order");
        _repository.Seed(existingPayment);

        var response = await _service.CreatePendingPaymentFromOrderAsync(
            PaymentTestData.OrderCreatedEvent("existing-order"));

        response.Id.Should().Be(existingPayment.Id);
        _publisher.PaymentApprovedCalls.Should().Be(0);
        _publisher.PaymentRejectedCalls.Should().Be(0);
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_rejectsTotalLessThanOrEqualToZero()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent(total: 0m);

        var action = () => _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        await action.Should().ThrowAsync<FinanceValidationException>();
    }

    [Theory]
    [InlineData("", "OrderId")]
    [InlineData(" ", "OrderId")]
    public async Task CreatePendingPaymentFromOrderAsync_WhenOrderIdIsMissing_ShouldThrowFinanceValidationException(
        string orderId,
        string expectedFieldName)
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent(orderId);

        var action = () => _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        await action.Should().ThrowAsync<FinanceValidationException>()
            .WithMessage($"{expectedFieldName} is required.");
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_WhenCustomerAccountIdIsMissing_ShouldThrowFinanceValidationException()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();
        orderEvent.CustomerAccountId = "";

        var action = () => _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        await action.Should().ThrowAsync<FinanceValidationException>()
            .WithMessage("CustomerAccountId is required.");
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_WhenRestaurantIdIsMissing_ShouldThrowFinanceValidationException()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();
        orderEvent.RestaurantId = " ";

        var action = () => _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        await action.Should().ThrowAsync<FinanceValidationException>()
            .WithMessage("RestaurantId is required.");
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_WhenBranchIdIsMissing_ShouldThrowFinanceValidationException()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent();
        orderEvent.BranchId = "";

        var action = () => _service.CreatePendingPaymentFromOrderAsync(orderEvent);

        await action.Should().ThrowAsync<FinanceValidationException>()
            .WithMessage("BranchId is required.");
    }

    [Fact]
    public async Task GetByIdAsync_returnsPayment()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.GetByIdAsync(payment.Id);

        response.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task GetByIdAsync_throwsNotFound()
    {
        var action = () => _service.GetByIdAsync(Guid.NewGuid());

        await action.Should().ThrowAsync<PaymentNotFoundException>();
    }

    [Fact]
    public async Task GetByOrderIdAsync_returnsPayment()
    {
        var payment = PaymentTestData.Payment(orderId: "order-by-id");
        _repository.Seed(payment);

        var response = await _service.GetByOrderIdAsync("order-by-id");

        response.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task GetByOrderIdAsync_WhenPaymentDoesNotExist_ShouldThrowPaymentNotFoundException()
    {
        var action = () => _service.GetByOrderIdAsync("missing-order");

        await action.Should().ThrowAsync<PaymentNotFoundException>();
    }

    [Fact]
    public async Task GetByCustomerAccountIdAsync_returnsPaymentsForCustomer()
    {
        _repository.Seed(PaymentTestData.Payment(orderId: "customer-order-1"));
        var otherCustomerPayment = PaymentTestData.Payment(orderId: "customer-order-2");
        otherCustomerPayment.CustomerAccountId = "customer-2";
        _repository.Seed(otherCustomerPayment);

        var response = await _service.GetByCustomerAccountIdAsync("customer-1");

        response.Should().ContainSingle()
            .Which.OrderId.Should().Be("customer-order-1");
    }

    [Fact]
    public async Task GetByRestaurantIdAsync_returnsPaymentsForRestaurant()
    {
        _repository.Seed(PaymentTestData.Payment(orderId: "restaurant-order-1"));
        var otherRestaurantPayment = PaymentTestData.Payment(orderId: "restaurant-order-2");
        otherRestaurantPayment.RestaurantId = "restaurant-2";
        _repository.Seed(otherRestaurantPayment);

        var response = await _service.GetByRestaurantIdAsync("restaurant-1");

        response.Should().ContainSingle()
            .Which.OrderId.Should().Be("restaurant-order-1");
    }

    [Fact]
    public async Task ApproveAsync_WhenPending_ShouldApproveAndPublishPaymentApproved()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.ApproveAsync(
            payment.Id,
            PaymentTestData.ApproveRequest("provider-ref-1"));

        response.Status.Should().Be(PaymentStatus.Approved);
        response.Provider.Should().Be("SIMULATED");
        response.ProviderReference.Should().Be("provider-ref-1");
        response.ApprovedAt.Should().NotBeNull();
        _publisher.PaymentApprovedCalls.Should().Be(1);
        _publisher.LastApprovedPayment!.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task ApproveAsync_WhenProviderReferenceIsBlank_ShouldGenerateSimulatedReference()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.ApproveAsync(
            payment.Id,
            PaymentTestData.ApproveRequest(" "));

        response.Status.Should().Be(PaymentStatus.Approved);
        response.ProviderReference.Should().StartWith("SIM-");
        _publisher.PaymentApprovedCalls.Should().Be(1);
    }

    [Fact]
    public async Task ApproveAsync_WhenPaymentDoesNotExist_ShouldThrowPaymentNotFoundExceptionAndNotPublish()
    {
        var action = () => _service.ApproveAsync(Guid.NewGuid(), PaymentTestData.ApproveRequest());

        await action.Should().ThrowAsync<PaymentNotFoundException>();
        _publisher.PaymentApprovedCalls.Should().Be(0);
    }

    [Fact]
    public async Task ApproveAsync_WhenPaymentNotPending_ShouldThrowAndNotPublish()
    {
        var payment = PaymentTestData.Payment(status: PaymentStatus.Rejected);
        _repository.Seed(payment);

        var action = () => _service.ApproveAsync(payment.Id, PaymentTestData.ApproveRequest());

        await action.Should().ThrowAsync<InvalidPaymentStatusTransitionException>();
        _publisher.PaymentApprovedCalls.Should().Be(0);
    }

    [Fact]
    public async Task RejectAsync_WhenPending_ShouldRejectAndPublishPaymentRejected()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.RejectAsync(
            payment.Id,
            PaymentTestData.RejectRequest("Card declined"));

        response.Status.Should().Be(PaymentStatus.Rejected);
        response.RejectionReason.Should().Be("Card declined");
        response.RejectedAt.Should().NotBeNull();
        _publisher.PaymentRejectedCalls.Should().Be(1);
        _publisher.LastRejectedPayment!.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task RejectAsync_WhenReasonIsEmpty_ShouldThrowAndNotPublish()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var action = () => _service.RejectAsync(payment.Id, new RejectPaymentRequest());

        await action.Should().ThrowAsync<FinanceValidationException>();
        _publisher.PaymentRejectedCalls.Should().Be(0);
    }

    [Fact]
    public async Task RejectAsync_WhenPaymentNotPending_ShouldThrowAndNotPublish()
    {
        var payment = PaymentTestData.Payment(status: PaymentStatus.Approved);
        _repository.Seed(payment);

        var action = () => _service.RejectAsync(payment.Id, PaymentTestData.RejectRequest());

        await action.Should().ThrowAsync<InvalidPaymentStatusTransitionException>();
        _publisher.PaymentRejectedCalls.Should().Be(0);
    }

    [Fact]
    public async Task RejectAsync_WhenPaymentDoesNotExist_ShouldThrowPaymentNotFoundExceptionAndNotPublish()
    {
        var action = () => _service.RejectAsync(Guid.NewGuid(), PaymentTestData.RejectRequest());

        await action.Should().ThrowAsync<PaymentNotFoundException>();
        _publisher.PaymentRejectedCalls.Should().Be(0);
    }

    [Fact]
    public async Task CancelAsync_WhenPending_ShouldCancelAndPublishPaymentCancelled()
    {
        var payment = PaymentTestData.Payment();
        _repository.Seed(payment);

        var response = await _service.CancelAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Cancelled);
        response.CancelledAt.Should().NotBeNull();
        _publisher.PaymentCancelledCalls.Should().Be(1);
        _publisher.LastCancelledPayment!.Id.Should().Be(payment.Id);
        _publisher.LastCancellationReason.Should().BeNull();
    }

    [Fact]
    public async Task CancelAsync_WhenPaymentNotPending_ShouldThrowAndNotPublish()
    {
        var payment = PaymentTestData.Payment(status: PaymentStatus.Approved);
        _repository.Seed(payment);

        var action = () => _service.CancelAsync(payment.Id);

        await action.Should().ThrowAsync<InvalidPaymentStatusTransitionException>();
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task CancelAsync_WhenPaymentDoesNotExist_ShouldThrowPaymentNotFoundExceptionAndNotPublish()
    {
        var action = () => _service.CancelAsync(Guid.NewGuid());

        await action.Should().ThrowAsync<PaymentNotFoundException>();
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_WhenCreateFailsButPaymentExists_ShouldReturnIdempotentPayment()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent("race-order");
        var idempotentPayment = PaymentTestData.Payment(orderId: "race-order");
        var repository = new Mock<IPaymentRepository>();
        repository
            .SetupSequence(repo => repo.FindByOrderIdAsync("race-order", It.IsAny<CancellationToken>()))
            .ReturnsAsync((FinanceService.Domain.Entities.Payment?)null)
            .ReturnsAsync(idempotentPayment);
        repository
            .Setup(repo => repo.CreateAsync(
                It.IsAny<FinanceService.Domain.Entities.Payment>(),
                It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("Duplicate order."));
        var service = new PaymentServiceImpl(
            repository.Object,
            Mock.Of<IPaymentEventPublisher>(),
            NullLogger<PaymentServiceImpl>.Instance);

        var response = await service.CreatePendingPaymentFromOrderAsync(orderEvent);

        response.Id.Should().Be(idempotentPayment.Id);
        response.OrderId.Should().Be("race-order");
    }

    [Fact]
    public async Task CreatePendingPaymentFromOrderAsync_WhenCreateFailsAndPaymentStillDoesNotExist_ShouldRethrow()
    {
        var orderEvent = PaymentTestData.OrderCreatedEvent("failed-race-order");
        var repository = new Mock<IPaymentRepository>();
        repository
            .Setup(repo => repo.FindByOrderIdAsync("failed-race-order", It.IsAny<CancellationToken>()))
            .ReturnsAsync((FinanceService.Domain.Entities.Payment?)null);
        repository
            .Setup(repo => repo.CreateAsync(
                It.IsAny<FinanceService.Domain.Entities.Payment>(),
                It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("Duplicate order."));
        var service = new PaymentServiceImpl(
            repository.Object,
            Mock.Of<IPaymentEventPublisher>(),
            NullLogger<PaymentServiceImpl>.Instance);

        var action = () => service.CreatePendingPaymentFromOrderAsync(orderEvent);

        await action.Should().ThrowAsync<InvalidOperationException>()
            .WithMessage("Duplicate order.");
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenOrderCancelledAndPaymentPending_ShouldCancelAndPublishPaymentCancelled()
    {
        var payment = PaymentTestData.Payment(orderId: "cancelled-order");
        _repository.Seed(payment);

        await _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("cancelled-order", "Cancelled"));

        var response = await _service.GetByIdAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Cancelled);
        _publisher.PaymentCancelledCalls.Should().Be(1);
        _publisher.LastCancelledPayment!.Id.Should().Be(payment.Id);
        _publisher.LastCancellationReason.Should().Be("ORDER_CANCELLED");
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenOrderCancelledAndPaymentMissing_ShouldNotThrowAndNotPublish()
    {
        var action = () => _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("missing-order", "Cancelled"));

        await action.Should().NotThrowAsync();
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenOrderCancelledAndPaymentApproved_ShouldNotChangeAndNotPublish()
    {
        var payment = PaymentTestData.Payment(orderId: "approved-order", status: PaymentStatus.Approved);
        _repository.Seed(payment);

        await _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("approved-order", "Cancelled"));

        var response = await _service.GetByIdAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Approved);
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenOrderCancelledAndPaymentAlreadyCancelled_ShouldNotPublishAgain()
    {
        var payment = PaymentTestData.Payment(orderId: "cancelled-order", status: PaymentStatus.Cancelled);
        _repository.Seed(payment);

        await _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("cancelled-order", "Cancelled"));

        var response = await _service.GetByIdAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Cancelled);
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }

    [Fact]
    public async Task HandleOrderStatusChangedAsync_WhenNewStatusIsNotCancelled_ShouldDoNothing()
    {
        var payment = PaymentTestData.Payment(orderId: "accepted-order");
        _repository.Seed(payment);

        await _service.HandleOrderStatusChangedAsync(
            PaymentTestData.OrderStatusChangedEvent("accepted-order", "Accepted"));

        var response = await _service.GetByIdAsync(payment.Id);

        response.Status.Should().Be(PaymentStatus.Pending);
        _publisher.PaymentCancelledCalls.Should().Be(0);
    }
}
