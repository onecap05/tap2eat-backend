using FinanceService.Dtos.Requests;
using FinanceService.Dtos.Responses;
using FinanceService.Services.Interfaces;
using Microsoft.AspNetCore.Mvc;

namespace FinanceService.Controllers;

[ApiController]
[Route("api/payments")]
public sealed class PaymentsController : ControllerBase
{
    private readonly IPaymentService _paymentService;

    public PaymentsController(IPaymentService paymentService)
    {
        _paymentService = paymentService;
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
        var payment = await _paymentService.ApproveAsync(id, request, cancellationToken);

        return Ok(payment);
    }

    [HttpPatch("{id:guid}/reject")]
    public async Task<ActionResult<PaymentResponse>> Reject(
        Guid id,
        [FromBody] RejectPaymentRequest request,
        CancellationToken cancellationToken)
    {
        var payment = await _paymentService.RejectAsync(id, request, cancellationToken);

        return Ok(payment);
    }

    [HttpPatch("{id:guid}/cancel")]
    public async Task<ActionResult<PaymentResponse>> Cancel(
        Guid id,
        CancellationToken cancellationToken)
    {
        var payment = await _paymentService.CancelAsync(id, cancellationToken);

        return Ok(payment);
    }
}
