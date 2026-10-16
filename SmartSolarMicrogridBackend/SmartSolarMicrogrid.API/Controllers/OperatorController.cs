// ============================================================================
// SmartSolar Microgrid System
// File: OperatorController.cs
// Purpose: API controllers for grid operator portal operations and administrative
//          branch/operator assignment management.
// ============================================================================

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Operator;
using SmartSolarMicrogrid.Application.Interfaces;

namespace SmartSolarMicrogrid.API.Controllers;

/// <summary>
/// Branch-scoped endpoints for the Grid Operator web portal and mobile app.
/// </summary>
[ApiController]
[Route("api/operator")]
[Authorize(Roles = "GridOperator")]
public class OperatorController(IOperatorService service) : ControllerBase
{
    private string UserId => User.FindFirstValue(ClaimTypes.NameIdentifier)
        ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)
        ?? "";

    [HttpGet("me")]
    public async Task<IActionResult> Me()
    {
        // Fetch operator profile and assigned station branch metadata
        var profile = await service.MeAsync(UserId);
        return Ok(profile);
    }

    [HttpGet("dashboard")]
    public async Task<IActionResult> Dashboard()
    {
        // Load branch-specific operational counts and slot summary
        var metrics = await service.DashboardAsync(UserId);
        return Ok(metrics);
    }

    [HttpGet("slots")]
    public async Task<IActionResult> Slots([FromQuery] bool availableOnly = false)
    {
        // Retrieve slots scoped strictly to the operator's assigned branch node
        var slots = await service.SlotsAsync(UserId, availableOnly);
        return Ok(slots);
    }

    [HttpGet("reservations/status/{status}")]
    public async Task<IActionResult> Reservations(string status)
    {
        // List reservations for this operator's branch by status (Pending, Approved, etc.)
        var reservations = await service.ReservationsAsync(UserId, status);
        return Ok(reservations);
    }

    [HttpPut("reservations/{id}/approve")]
    public async Task<IActionResult> Approve(string id)
    {
        // Operator approval for a reservation booked at their station branch
        var approved = await service.ApproveReservationAsync(UserId, id);
        return Ok(approved);
    }

    [HttpGet("slot-requests")]
    public async Task<IActionResult> MyRequests()
    {
        // View history of slot unavailability requests submitted by this operator
        var requests = await service.MySlotRequestsAsync(UserId);
        return Ok(requests);
    }

    [HttpPost("slot-requests")]
    public async Task<IActionResult> CreateRequest(CreateSlotRequestDto d)
    {
        // Request backoffice approval to mark a slot unavailable due to maintenance
        var request = await service.CreateSlotRequestAsync(UserId, d);
        return Ok(request);
    }
}

/// <summary>
/// Backoffice administration: review slot unavailability requests and assign operators.
/// </summary>
[ApiController]
[Authorize(Roles = "Backoffice")]
public class OperatorAdminController(IOperatorService service) : ControllerBase
{
    [HttpGet("api/slot-requests")]
    public async Task<IActionResult> Requests([FromQuery] string? status = null)
    {
        // Review pending, approved, or rejected slot unavailability requests
        var requests = await service.SlotRequestsAsync(status);
        return Ok(requests);
    }

    [HttpPut("api/slot-requests/{id}/approve")]
    public async Task<IActionResult> ApproveRequest(string id, [FromBody] ResolveSlotRequestDto? d = null)
    {
        // Approve operator's maintenance request and lock the energy slot
        var resolved = await service.ApproveSlotRequestAsync(id, d?.AdminNote);
        return Ok(resolved);
    }

    [HttpPut("api/slot-requests/{id}/reject")]
    public async Task<IActionResult> RejectRequest(string id, [FromBody] ResolveSlotRequestDto? d = null)
    {
        // Reject operator's unavailability request and keep the slot active
        var rejected = await service.RejectSlotRequestAsync(id, d?.AdminNote);
        return Ok(rejected);
    }

    [HttpGet("api/operator-assignments")]
    public async Task<IActionResult> Assignments()
    {
        // View all operators and their assigned microgrid station branches
        var list = await service.AssignmentsAsync();
        return Ok(list);
    }

    [HttpPut("api/operator-assignments/{userId}")]
    public async Task<IActionResult> Assign(string userId, AssignStationDto d)
    {
        // Assign or reassign a grid operator to a station node
        var assignment = await service.AssignStationAsync(userId, d.StationId);
        return Ok(assignment);
    }
}
