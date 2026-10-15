using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Operator;
using SmartSolarMicrogrid.Application.Interfaces;
namespace SmartSolarMicrogrid.API.Controllers;

// Branch-scoped endpoints for the Grid Operator web portal.
[ApiController,Route("api/operator"),Authorize(Roles="GridOperator")]
public class OperatorController(IOperatorService service):ControllerBase {
    private string UserId=>User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub) ?? "";
    [HttpGet("me")] public async Task<IActionResult> Me()=>Ok(await service.MeAsync(UserId));
    [HttpGet("dashboard")] public async Task<IActionResult> Dashboard()=>Ok(await service.DashboardAsync(UserId));
    [HttpGet("slots")] public async Task<IActionResult> Slots()=>Ok(await service.SlotsAsync(UserId));
    [HttpGet("reservations/status/{status}")] public async Task<IActionResult> Reservations(string status)=>Ok(await service.ReservationsAsync(UserId,status));
    [HttpPut("reservations/{id}/approve")] public async Task<IActionResult> Approve(string id)=>Ok(await service.ApproveReservationAsync(UserId,id));
    [HttpGet("slot-requests")] public async Task<IActionResult> MyRequests()=>Ok(await service.MySlotRequestsAsync(UserId));
    [HttpPost("slot-requests")] public async Task<IActionResult> CreateRequest(CreateSlotRequestDto d)=>Ok(await service.CreateSlotRequestAsync(UserId,d));
}

// Backoffice side: review slot unavailability requests and assign operators to branches.
[ApiController,Authorize(Roles="Backoffice")]
public class OperatorAdminController(IOperatorService service):ControllerBase {
    [HttpGet("api/slot-requests")] public async Task<IActionResult> Requests([FromQuery]string? status=null)=>Ok(await service.SlotRequestsAsync(status));
    [HttpPut("api/slot-requests/{id}/approve")] public async Task<IActionResult> ApproveRequest(string id,[FromBody]ResolveSlotRequestDto? d=null)=>Ok(await service.ApproveSlotRequestAsync(id,d?.AdminNote));
    [HttpPut("api/slot-requests/{id}/reject")] public async Task<IActionResult> RejectRequest(string id,[FromBody]ResolveSlotRequestDto? d=null)=>Ok(await service.RejectSlotRequestAsync(id,d?.AdminNote));
    [HttpGet("api/operator-assignments")] public async Task<IActionResult> Assignments()=>Ok(await service.AssignmentsAsync());
    [HttpPut("api/operator-assignments/{userId}")] public async Task<IActionResult> Assign(string userId,AssignStationDto d)=>Ok(await service.AssignStationAsync(userId,d.StationId));
}
