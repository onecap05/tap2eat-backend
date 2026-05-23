using FinanceService.Controllers;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Security;
using FinanceService.Services.Interfaces;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace FinanceService.Tests.Controllers;

public sealed class PaymentsControllerTests
{
    private readonly Mock<IPaymentService> _paymentService = new();
    private readonly Mock<IPaymentSimulationTokenValidator> _tokenValidator = new();

    [Fact]
    public async Task GetById_returnsOk()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Pending);
        _paymentService
            .Setup(service => service.GetByIdAsync(paymentId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = CreateController();

        var result = await controller.GetById(paymentId, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
    }

    [Fact]
    public async Task Approve_withoutHeader_returnsForbidden()
    {
        _tokenValidator
            .Setup(validator => validator.IsValid(null))
            .Returns(false);

        var controller = CreateController();

        var result = await controller.Approve(
            Guid.NewGuid(),
            new ApprovePaymentRequest(),
            CancellationToken.None);

        result.Result.Should().BeOfType<StatusCodeResult>()
            .Which.StatusCode.Should().Be(StatusCodes.Status403Forbidden);
        _paymentService.Verify(
            service => service.ApproveAsync(
                It.IsAny<Guid>(),
                It.IsAny<ApprovePaymentRequest>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task Approve_withIncorrectHeader_returnsForbidden()
    {
        _tokenValidator
            .Setup(validator => validator.IsValid("wrong-token"))
            .Returns(false);

        var controller = CreateController("wrong-token");

        var result = await controller.Approve(
            Guid.NewGuid(),
            new ApprovePaymentRequest(),
            CancellationToken.None);

        result.Result.Should().BeOfType<StatusCodeResult>()
            .Which.StatusCode.Should().Be(StatusCodes.Status403Forbidden);
        _paymentService.Verify(
            service => service.ApproveAsync(
                It.IsAny<Guid>(),
                It.IsAny<ApprovePaymentRequest>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task Approve_withCorrectHeader_returnsApprovedPayment()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Approved);
        var request = new ApprovePaymentRequest { ProviderReference = "ref-1" };
        _tokenValidator
            .Setup(validator => validator.IsValid("valid-token"))
            .Returns(true);
        _paymentService
            .Setup(service => service.ApproveAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = CreateController("valid-token");

        var result = await controller.Approve(paymentId, request, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
    }

    [Fact]
    public async Task Reject_withoutHeader_returnsForbidden()
    {
        _tokenValidator
            .Setup(validator => validator.IsValid(null))
            .Returns(false);

        var controller = CreateController();

        var result = await controller.Reject(
            Guid.NewGuid(),
            new RejectPaymentRequest { RejectionReason = "Declined" },
            CancellationToken.None);

        result.Result.Should().BeOfType<StatusCodeResult>()
            .Which.StatusCode.Should().Be(StatusCodes.Status403Forbidden);
        _paymentService.Verify(
            service => service.RejectAsync(
                It.IsAny<Guid>(),
                It.IsAny<RejectPaymentRequest>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task Reject_withCorrectHeader_returnsRejectedPayment()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Rejected);
        var request = new RejectPaymentRequest { RejectionReason = "Declined" };
        _tokenValidator
            .Setup(validator => validator.IsValid("valid-token"))
            .Returns(true);
        _paymentService
            .Setup(service => service.RejectAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = CreateController("valid-token");

        var result = await controller.Reject(paymentId, request, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
    }

    [Fact]
    public async Task Cancel_withoutHeader_returnsForbidden()
    {
        _tokenValidator
            .Setup(validator => validator.IsValid(null))
            .Returns(false);

        var controller = CreateController();

        var result = await controller.Cancel(Guid.NewGuid(), CancellationToken.None);

        result.Result.Should().BeOfType<StatusCodeResult>()
            .Which.StatusCode.Should().Be(StatusCodes.Status403Forbidden);
        _paymentService.Verify(
            service => service.CancelAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task Cancel_withCorrectHeader_returnsCancelledPayment()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Cancelled);
        _tokenValidator
            .Setup(validator => validator.IsValid("valid-token"))
            .Returns(true);
        _paymentService
            .Setup(service => service.CancelAsync(paymentId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = CreateController("valid-token");

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

    private PaymentsController CreateController(string? simulationToken = null)
    {
        var httpContext = new DefaultHttpContext();

        if (simulationToken is not null)
        {
            httpContext.Request.Headers["X-Simulated-Payment-Token"] = simulationToken;
        }

        return new PaymentsController(_paymentService.Object, _tokenValidator.Object)
        {
            ControllerContext = new ControllerContext
            {
                HttpContext = httpContext
            }
        };
    }
}
