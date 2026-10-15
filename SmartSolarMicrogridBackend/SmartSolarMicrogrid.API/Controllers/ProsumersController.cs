using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Prosumer;
using SmartSolarMicrogrid.Application.Interfaces;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/prosumers")]
public class ProsumersController(IProsumerService service):ControllerBase {
    [AllowAnonymous,HttpPost("register")] public async Task<IActionResult> Register(CreateProsumerDto d)=>Ok(await service.RegisterAsync(d));
    [Authorize,HttpGet("{nic}")] public async Task<IActionResult> Get(string nic)=>Ok(await service.GetByNicAsync(nic));
    [Authorize(Roles="Prosumer"),HttpPut("{nic}")] public async Task<IActionResult> Update(string nic,UpdateProsumerDto d)=>Ok(await service.UpdateAsync(nic,d));
    [Authorize(Roles="Prosumer"),HttpPost("{nic}/deactivation-request")] public async Task<IActionResult> DeactivateRequest(string nic)=>Ok(await service.RequestDeactivationAsync(nic));
    [Authorize(Roles="Backoffice"),HttpGet("pending")] public async Task<IActionResult> Pending()=>Ok(await service.GetPendingAsync());
    [Authorize(Roles="Backoffice"),HttpGet("all")] public async Task<IActionResult> All()=>Ok(await service.GetAllAsync());
    [Authorize(Roles="Backoffice"),HttpPut("{nic}/activate")] public async Task<IActionResult> Activate(string nic)=>Ok(await service.SetStatusAsync(nic,"activate"));
    [Authorize(Roles="Backoffice"),HttpPut("{nic}/verify-nic")] public async Task<IActionResult> VerifyNic(string nic)=>Ok(await service.SetStatusAsync(nic,"verify-nic"));
    [Authorize(Roles="Backoffice"),HttpPut("{nic}/deactivate")] public async Task<IActionResult> Deactivate(string nic)=>Ok(await service.SetStatusAsync(nic,"deactivate"));
    [Authorize(Roles="Backoffice"),HttpPut("{nic}/reactivate")] public async Task<IActionResult> Reactivate(string nic)=>Ok(await service.SetStatusAsync(nic,"reactivate"));
    [Authorize(Roles="Backoffice"),HttpPut("{nic}/reject")] public async Task<IActionResult> Reject(string nic)=>Ok(await service.SetStatusAsync(nic,"reject"));
}
