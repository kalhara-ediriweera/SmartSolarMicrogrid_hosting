using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Reservation;
using SmartSolarMicrogrid.Application.Interfaces;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/reservations")]
public class ReservationsController(IReservationService service,IProsumerService prosumers):ControllerBase {
    private string Nic=>User.FindFirstValue("nic") ?? User.FindFirstValue(ClaimTypes.Name) ?? "";
    [Authorize(Roles="Prosumer"),HttpPost] public async Task<IActionResult> Create([FromQuery]string nic,CreateReservationDto d)=>Ok(await service.CreateAsync(nic,d));
    [Authorize,HttpGet("{id}")] public async Task<IActionResult> Get(string id)=>Ok(await service.GetAsync(id));
    [Authorize(Roles="Prosumer"),HttpGet("mine")] public async Task<IActionResult> Mine([FromQuery]string nic,[FromQuery]string? search=null,[FromQuery]string? status=null)=>Ok(await service.GetMineAsync(nic,search,status));
    [Authorize(Roles="Backoffice,GridOperator"),HttpGet("status/{status}")] public async Task<IActionResult> ByStatus(string status)=>Ok(await service.GetByStatusAsync(status));
    [Authorize(Roles="Backoffice,GridOperator"),HttpGet("all")] public async Task<IActionResult> GetAll()=>Ok(await service.GetAllAsync());
    [Authorize(Roles="Prosumer"),HttpPut("{id}")] public async Task<IActionResult> Update(string id,[FromQuery]string nic,UpdateReservationDto d)=>Ok(await service.UpdateAsync(nic,id,d));
    [Authorize(Roles="Prosumer"),HttpDelete("{id}")] public async Task<IActionResult> Cancel(string id,[FromQuery]string nic)=>Ok(await service.CancelAsync(nic,id));
    [Authorize(Roles="Backoffice,GridOperator"),HttpPut("{id}/approve")] public async Task<IActionResult> Approve(string id)=>Ok(await service.ApproveAsync(id));
}
