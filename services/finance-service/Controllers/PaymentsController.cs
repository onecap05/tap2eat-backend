using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Security;
using FinanceService.Services.Interfaces;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FinanceService.Controllers;

[ApiController]
[Authorize]
[Route("api/payments")]
public sealed class PaymentsController : ControllerBase
{
    private const string SimulatedPaymentTokenHeaderName = "X-Simulated-Payment-Token";

    private readonly IPaymentService _paymentService;
    private readonly IPayPalPaymentService _payPalPaymentService;
    private readonly IPaymentSimulationTokenValidator _paymentSimulationTokenValidator;

    public PaymentsController(
        IPaymentService paymentService,
        IPayPalPaymentService payPalPaymentService,
        IPaymentSimulationTokenValidator paymentSimulationTokenValidator)
    {
        _paymentService = paymentService;
        _payPalPaymentService = payPalPaymentService;
        _paymentSimulationTokenValidator = paymentSimulationTokenValidator;
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<PaymentResponse>> GetById(
        Guid id,
        CancellationToken cancellationToken)
    {
        var payment = await _paymentService.GetByIdAsync(id, cancellationToken);

        return Ok(payment);
    }

    [HttpGet("order/{orderId}")]
    public async Task<ActionResult<PaymentResponse>> GetByOrderId(
        string orderId,
        CancellationToken cancellationToken)
    {
        var payment = await _paymentService.GetByOrderIdAsync(orderId, cancellationToken);

        return Ok(payment);
    }

    [HttpGet("customer/{customerAccountId}")]
    public async Task<ActionResult<IReadOnlyList<PaymentResponse>>> GetByCustomerAccountId(
        string customerAccountId,
        CancellationToken cancellationToken)
    {
        var payments = await _paymentService.GetByCustomerAccountIdAsync(
            customerAccountId,
            cancellationToken);

        return Ok(payments);
    }

    [HttpGet("restaurant/{restaurantId}")]
    public async Task<ActionResult<IReadOnlyList<PaymentResponse>>> GetByRestaurantId(
        string restaurantId,
        CancellationToken cancellationToken)
    {
        var payments = await _paymentService.GetByRestaurantIdAsync(
            restaurantId,
            cancellationToken);

        return Ok(payments);
    }

    [HttpPatch("{id:guid}/approve")]
    public async Task<ActionResult<PaymentResponse>> Approve(
        Guid id,
        [FromBody] ApprovePaymentRequest request,
        CancellationToken cancellationToken)
    {
        if (!HasValidSimulationToken())
        {
            return StatusCode(StatusCodes.Status403Forbidden);
        }

        var payment = await _paymentService.ApproveAsync(id, request, cancellationToken);

        return Ok(payment);
    }

    [HttpPatch("{id:guid}/cash/confirm")]
    public async Task<ActionResult<PaymentResponse>> ConfirmCashPayment(
        Guid id,
        [FromBody] ConfirmCashPaymentRequest request,
        CancellationToken cancellationToken)
    {
        var payment = await _paymentService.ConfirmCashPaymentAsync(
            id,
            request,
            cancellationToken);

        return Ok(payment);
    }

    [HttpPatch("{id:guid}/reject")]
    public async Task<ActionResult<PaymentResponse>> Reject(
        Guid id,
        [FromBody] RejectPaymentRequest request,
        CancellationToken cancellationToken)
    {
        if (!HasValidSimulationToken())
        {
            return StatusCode(StatusCodes.Status403Forbidden);
        }

        var payment = await _paymentService.RejectAsync(id, request, cancellationToken);

        return Ok(payment);
    }

    [HttpPatch("{id:guid}/cancel")]
    public async Task<ActionResult<PaymentResponse>> Cancel(
        Guid id,
        CancellationToken cancellationToken)
    {
        if (!HasValidSimulationToken())
        {
            return StatusCode(StatusCodes.Status403Forbidden);
        }

        var payment = await _paymentService.CancelAsync(id, cancellationToken);

        return Ok(payment);
    }

    [HttpPost("{paymentId:guid}/paypal/create-order")]
    public async Task<ActionResult<PayPalOrderResponse>> CreatePayPalOrder(
        Guid paymentId,
        [FromBody] CreatePayPalOrderRequest? request,
        CancellationToken cancellationToken)
    {
        var response = await _payPalPaymentService.CreateOrderAsync(paymentId, cancellationToken);

        return Ok(response);
    }

    [HttpPost("{paymentId:guid}/paypal/capture")]
    public async Task<ActionResult<PayPalCaptureResponse>> CapturePayPalOrder(
        Guid paymentId,
        [FromBody] CapturePayPalOrderRequest request,
        CancellationToken cancellationToken)
    {
        var response = await _payPalPaymentService.CaptureOrderAsync(
            paymentId,
            request,
            cancellationToken);

        return Ok(response);
    }

    private bool HasValidSimulationToken()
    {
        var token = Request.Headers[SimulatedPaymentTokenHeaderName].FirstOrDefault();

        return _paymentSimulationTokenValidator.IsValid(token);
    }
}
