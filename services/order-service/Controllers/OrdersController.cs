using Microsoft.AspNetCore.Mvc;
using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Services.Interfaces;

namespace OrderService.Controllers;

[ApiController]
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
        var createdOrder = await _orderService.CreateAsync(request, cancellationToken);

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
        var order = await _orderService.GetByIdAsync(id, cancellationToken);

        return Ok(order);
    }

    [HttpGet("customer/{customerAccountId}")]
    public async Task<ActionResult<IReadOnlyList<OrderResponse>>> GetByCustomerAccountId(
        string customerAccountId,
        [FromQuery] OrderQueryRequest query,
        CancellationToken cancellationToken)
    {
        var orders = await _orderService.GetByCustomerAccountIdAsync(
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
        var orders = await _orderService.GetByRestaurantIdAsync(
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
        var orders = await _orderService.GetByBranchIdAsync(
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
