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

        if (order is null)
        {
            return NotFound(new
            {
                message = "Order not found."
            });
        }

        return Ok(order);
    }
}