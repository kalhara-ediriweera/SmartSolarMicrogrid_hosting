using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.Interfaces;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/dashboard")]
public class DashboardController(IDashboardService service):ControllerBase {
    [Authorize(Roles="Prosumer"),HttpGet("prosumer")] public async Task<IActionResult> Prosumer([FromQuery]string nic)=>Ok(await service.ProsumerAsync(nic));
    [Authorize(Roles="GridOperator"),HttpGet("operator")] public async Task<IActionResult> Operator()=>Ok(await service.OperatorAsync());
    [Authorize(Roles="Backoffice"),HttpGet("backoffice")] public async Task<IActionResult> Backoffice()=>Ok(await service.BackofficeAsync());
}
