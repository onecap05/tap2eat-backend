using FinanceService.Controllers;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Services.Interfaces;
using FluentAssertions;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace FinanceService.Tests.Controllers;

public sealed class PaymentsControllerTests
{
    private readonly Mock<IPaymentService> _paymentService = new();

    [Fact]
    public async Task GetById_returnsOk()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Pending);
        _paymentService
            .Setup(service => service.GetByIdAsync(paymentId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = new PaymentsController(_paymentService.Object);

        var result = await controller.GetById(paymentId, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
    }

    [Fact]
    public async Task Approve_returnsApprovedPayment()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Approved);
        var request = new ApprovePaymentRequest { ProviderReference = "ref-1" };
        _paymentService
            .Setup(service => service.ApproveAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = new PaymentsController(_paymentService.Object);

        var result = await controller.Approve(paymentId, request, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
    }

    [Fact]
    public async Task Reject_returnsRejectedPayment()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Rejected);
        var request = new RejectPaymentRequest { RejectionReason = "Declined" };
        _paymentService
            .Setup(service => service.RejectAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = new PaymentsController(_paymentService.Object);

        var result = await controller.Reject(paymentId, request, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
    }

    [Fact]
    public async Task Cancel_returnsCancelledPayment()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Cancelled);
        _paymentService
            .Setup(service => service.CancelAsync(paymentId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = new PaymentsController(_paymentService.Object);

        var result = await controller.Cancel(paymentId, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
    }

    private static PaymentResponse Response(Guid id, PaymentStatus status)
    {
        return new PaymentResponse
        {
            Id = id,
            OrderId = "order-1",
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            Amount = 150.75m,
            Currency = "MXN",
            Status = status,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
    }
}
