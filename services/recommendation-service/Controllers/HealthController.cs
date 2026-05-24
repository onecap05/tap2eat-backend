using Microsoft.AspNetCore.Mvc;

namespace RecommendationService.Controllers;

[ApiController]
[Microsoft.AspNetCore.Authorization.AllowAnonymous]
[Route("api/recommendations")]
public class HealthController : ControllerBase
{
    [HttpGet("health")]
    public IActionResult Health()
    {
        return Ok(new
        {
            service = "recommendation-service",
            status = "UP"
        });
    }
}
