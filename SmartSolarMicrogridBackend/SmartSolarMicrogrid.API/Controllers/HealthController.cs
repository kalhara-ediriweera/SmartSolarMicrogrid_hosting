using Microsoft.AspNetCore.Mvc;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/health")]
public class HealthController:ControllerBase {
    [HttpGet] public IActionResult Get()=>Ok(new{status="ok",service="SmartSolarMicrogridAPI",utc=DateTime.UtcNow});
}
