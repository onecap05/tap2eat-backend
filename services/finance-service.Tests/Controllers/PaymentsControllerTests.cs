using FinanceService.Controllers;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Exceptions;
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
    private readonly Mock<IPayPalPaymentService> _payPalPaymentService = new();
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
    public async Task GetById_WhenPaymentDoesNotExist_ThrowsPaymentNotFoundException()
    {
        var paymentId = Guid.NewGuid();
        _paymentService
            .Setup(service => service.GetByIdAsync(paymentId, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new PaymentNotFoundException(paymentId.ToString()));

        var controller = CreateController();

        var action = () => controller.GetById(paymentId, CancellationToken.None);

        await action.Should().ThrowAsync<PaymentNotFoundException>();
    }

    [Fact]
    public async Task GetByOrderId_returnsOk()
    {
        var expectedPayment = Response(Guid.NewGuid(), PaymentStatus.Pending);
        _paymentService
            .Setup(service => service.GetByOrderIdAsync("order-1", It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = CreateController();

        var result = await controller.GetByOrderId("order-1", CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
    }

    [Fact]
    public async Task GetByCustomerAccountId_returnsOk()
    {
        var expectedPayments = new[] { Response(Guid.NewGuid(), PaymentStatus.Pending) };
        _paymentService
            .Setup(service => service.GetByCustomerAccountIdAsync("customer-1", It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayments);

        var controller = CreateController();

        var result = await controller.GetByCustomerAccountId("customer-1", CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayments);
    }

    [Fact]
    public async Task GetByRestaurantId_returnsOk()
    {
        var expectedPayments = new[] { Response(Guid.NewGuid(), PaymentStatus.Pending) };
        _paymentService
            .Setup(service => service.GetByRestaurantIdAsync("restaurant-1", It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayments);

        var controller = CreateController();

        var result = await controller.GetByRestaurantId("restaurant-1", CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayments);
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
    public async Task Approve_WithInvalidRequestAndValidToken_PropagatesFinanceValidationException()
    {
        var paymentId = Guid.NewGuid();
        var request = new ApprovePaymentRequest();
        _tokenValidator
            .Setup(validator => validator.IsValid("valid-token"))
            .Returns(true);
        _paymentService
            .Setup(service => service.ApproveAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new FinanceValidationException("Invalid request."));

        var controller = CreateController("valid-token");

        var action = () => controller.Approve(paymentId, request, CancellationToken.None);

        await action.Should().ThrowAsync<FinanceValidationException>();
    }

    [Fact]
    public async Task ConfirmCashPayment_returnsApprovedPayment()
    {
        var paymentId = Guid.NewGuid();
        var expectedPayment = Response(paymentId, PaymentStatus.Approved);
        expectedPayment.Provider = "CASH";
        expectedPayment.AmountReceived = 200m;
        expectedPayment.ChangeAmount = 49.25m;
        var request = new ConfirmCashPaymentRequest { AmountReceived = 200m };
        _paymentService
            .Setup(service => service.ConfirmCashPaymentAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedPayment);

        var controller = CreateController();

        var result = await controller.ConfirmCashPayment(paymentId, request, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedPayment);
        _tokenValidator.Verify(validator => validator.IsValid(It.IsAny<string?>()), Times.Never);
    }

    [Fact]
    public async Task ConfirmCashPayment_WithInvalidRequest_PropagatesFinanceValidationException()
    {
        var paymentId = Guid.NewGuid();
        var request = new ConfirmCashPaymentRequest { AmountReceived = 100m };
        _paymentService
            .Setup(service => service.ConfirmCashPaymentAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new FinanceValidationException("Amount received must be greater than or equal to the payment amount."));

        var controller = CreateController();

        var action = () => controller.ConfirmCashPayment(paymentId, request, CancellationToken.None);

        await action.Should().ThrowAsync<FinanceValidationException>();
        _tokenValidator.Verify(validator => validator.IsValid(It.IsAny<string?>()), Times.Never);
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
    public async Task Reject_WithInvalidRequestAndValidToken_PropagatesFinanceValidationException()
    {
        var paymentId = Guid.NewGuid();
        var request = new RejectPaymentRequest();
        _tokenValidator
            .Setup(validator => validator.IsValid("valid-token"))
            .Returns(true);
        _paymentService
            .Setup(service => service.RejectAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new FinanceValidationException("Rejection reason is required."));

        var controller = CreateController("valid-token");

        var action = () => controller.Reject(paymentId, request, CancellationToken.None);

        await action.Should().ThrowAsync<FinanceValidationException>();
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

    [Fact]
    public async Task Cancel_WhenPaymentDoesNotExistAndTokenIsValid_PropagatesPaymentNotFoundException()
    {
        var paymentId = Guid.NewGuid();
        _tokenValidator
            .Setup(validator => validator.IsValid("valid-token"))
            .Returns(true);
        _paymentService
            .Setup(service => service.CancelAsync(paymentId, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new PaymentNotFoundException(paymentId.ToString()));

        var controller = CreateController("valid-token");

        var action = () => controller.Cancel(paymentId, CancellationToken.None);

        await action.Should().ThrowAsync<PaymentNotFoundException>();
    }

    [Fact]
    public async Task CreatePayPalOrder_withPaymentId_returnsOk()
    {
        var paymentId = Guid.NewGuid();
        var expectedResponse = new PayPalOrderResponse
        {
            PaymentId = paymentId,
            PaypalOrderId = "PAYPAL-ORDER-1",
            Status = PaymentStatus.Pending.ToString(),
            Amount = 150.75m,
            Currency = "MXN"
        };
        _payPalPaymentService
            .Setup(service => service.CreateOrderAsync(paymentId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedResponse);

        var controller = CreateController();

        var result = await controller.CreatePayPalOrder(
            paymentId,
            new CreatePayPalOrderRequest(),
            CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedResponse);
        _tokenValidator.Verify(validator => validator.IsValid(It.IsAny<string?>()), Times.Never);
    }

    [Fact]
    public async Task CapturePayPalOrder_withPaymentId_returnsOk()
    {
        var paymentId = Guid.NewGuid();
        var request = new CapturePayPalOrderRequest { PaypalOrderId = "PAYPAL-ORDER-1" };
        var expectedResponse = new PayPalCaptureResponse
        {
            PaymentId = paymentId,
            PaypalOrderId = "PAYPAL-ORDER-1",
            CaptureId = "CAPTURE-1",
            PaymentStatus = PaymentStatus.Approved,
            ProviderReference = "CAPTURE-1"
        };
        _payPalPaymentService
            .Setup(service => service.CaptureOrderAsync(paymentId, request, It.IsAny<CancellationToken>()))
            .ReturnsAsync(expectedResponse);

        var controller = CreateController();

        var result = await controller.CapturePayPalOrder(paymentId, request, CancellationToken.None);

        result.Result.Should().BeOfType<OkObjectResult>()
            .Which.Value.Should().Be(expectedResponse);
        _tokenValidator.Verify(validator => validator.IsValid(It.IsAny<string?>()), Times.Never);
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

        return new PaymentsController(
            _paymentService.Object,
            _payPalPaymentService.Object,
            _tokenValidator.Object)
        {
            ControllerContext = new ControllerContext
            {
                HttpContext = httpContext
            }
        };
    }
}
