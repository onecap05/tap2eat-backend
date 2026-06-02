using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Services.Interfaces;

namespace OrderService.Controllers;

[ApiController]
[Authorize]
[Route("api/orders")]
public sealed class OrdersController : ControllerBase
{
    private readonly IOrderService _orderService;

    public OrdersController(IOrderService orderService)
    {
        _orderService = orderService;
    }

    [HttpPost]
    public async Task<ActionResult<OrderResponse>> Create(
        [FromBody] CreateOrderRequest request,
        CancellationToken cancellationToken)
    {
        var createdOrder = await _orderService.CreateOrderAsync(request, cancellationToken);

        return CreatedAtAction(
            nameof(GetById),
            new { id = createdOrder.Id },
            createdOrder);
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<OrderResponse>> GetById(
        string id,
        CancellationToken cancellationToken)
    {
        var order = await _orderService.GetOrderByIdAsync(id, cancellationToken);

        return Ok(order);
    }

    [AllowAnonymous]
    [HttpGet("public/track/{publicTrackingCode}")]
    public async Task<ActionResult<PublicOrderTrackingResponse>> GetPublicTracking(
        string publicTrackingCode,
        CancellationToken cancellationToken)
    {
        var order = await _orderService.GetOrderPublicTrackingAsync(
            publicTrackingCode,
            cancellationToken);

        return Ok(order);
    }

    [HttpGet("customer/{customerAccountId}")]
    public async Task<ActionResult<IReadOnlyList<OrderResponse>>> GetByCustomerAccountId(
        string customerAccountId,
        [FromQuery] OrderQueryRequest query,
        CancellationToken cancellationToken)
    {
        var orders = await _orderService.GetOrderByCustomerAccountIdAsync(
            customerAccountId,
            query,
            cancellationToken);

        return Ok(orders);
    }

    [HttpGet("restaurant/{restaurantId}")]
    public async Task<ActionResult<IReadOnlyList<OrderResponse>>> GetByRestaurantId(
        string restaurantId,
        [FromQuery] OrderQueryRequest query,
        CancellationToken cancellationToken)
    {
        var orders = await _orderService.GetOrderByRestaurantIdAsync(
            restaurantId,
            query,
            cancellationToken);

        return Ok(orders);
    }

    [HttpGet("branch/{branchId}")]
    public async Task<ActionResult<IReadOnlyList<OrderResponse>>> GetByBranchId(
        string branchId,
        [FromQuery] OrderQueryRequest query,
        CancellationToken cancellationToken)
    {
        var orders = await _orderService.GetOrderByBranchIdAsync(
            branchId,
            query,
            cancellationToken);

        return Ok(orders);
    }

    [HttpPatch("{id}/status")]
    public async Task<ActionResult<OrderResponse>> UpdateStatus(
        string id,
        [FromBody] UpdateOrderStatusRequest request,
        CancellationToken cancellationToken)
    {
        var order = await _orderService.UpdateStatusAsync(id, request, cancellationToken);

        return Ok(order);
    }
}
