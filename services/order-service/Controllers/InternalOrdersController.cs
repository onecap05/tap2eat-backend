using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using OrderService.Config;
using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Services.Interfaces;

namespace OrderService.Controllers;

[ApiController]
[AllowAnonymous]
[Route("api/internal/orders")]
public sealed class InternalOrdersController : ControllerBase
{
    private const string InternalServiceTokenHeader = "X-Internal-Service-Token";

    private readonly IOrderService _orderService;
    private readonly InternalServiceSettings _internalServiceSettings;

    public InternalOrdersController(
        IOrderService orderService,
        IOptions<InternalServiceSettings> internalServiceSettings)
    {
        _orderService = orderService;
        _internalServiceSettings = internalServiceSettings.Value;
    }

    [HttpGet("restaurant/{restaurantId}")]
    public async Task<ActionResult<IReadOnlyList<OrderResponse>>> GetByRestaurantId(
        string restaurantId,
        [FromQuery] OrderQueryRequest query,
        CancellationToken cancellationToken)
    {
        if (!IsInternalRequestAuthorized())
        {
            return Unauthorized();
        }

        var orders = await _orderService.GetByRestaurantIdAsync(
            restaurantId,
            query,
            cancellationToken);

        return Ok(orders);
    }

    private bool IsInternalRequestAuthorized()
    {
        if (!Request.Headers.TryGetValue(InternalServiceTokenHeader, out var providedToken))
        {
            return false;
        }

        return !string.IsNullOrWhiteSpace(_internalServiceSettings.Token)
            && providedToken == _internalServiceSettings.Token;
    }
}