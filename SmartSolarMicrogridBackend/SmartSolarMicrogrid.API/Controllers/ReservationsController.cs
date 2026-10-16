// ============================================================================
// SmartSolar Microgrid System
// File: ReservationsController.cs
// Purpose: API controller managing prosumer slot bookings, updates, cancellations,
//          and operator/backoffice approval workflows.
// ============================================================================

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Reservation;
using SmartSolarMicrogrid.Application.Interfaces;

namespace SmartSolarMicrogrid.API.Controllers;

[ApiController]
[Route("api/reservations")]
public class ReservationsController(IReservationService service, IProsumerService prosumers) : ControllerBase
{
    private string Nic => User.FindFirstValue("nic")
        ?? User.FindFirstValue(ClaimTypes.Name)
        ?? "";

    [Authorize(Roles = "Prosumer")]
    [HttpPost]
    public async Task<IActionResult> Create([FromQuery] string nic, CreateReservationDto d)
    {
        // Reserve an upcoming energy slot within the 7-day future booking window
        var reservation = await service.CreateAsync(nic, d);
        return Ok(reservation);
    }

    [Authorize]
    [HttpGet("{id}")]
    public async Task<IActionResult> Get(string id)
    {
        // Retrieve single reservation details by its unique identifier
        var reservation = await service.GetAsync(id);
        return Ok(reservation);
    }

    [Authorize(Roles = "Prosumer")]
    [HttpGet("mine")]
    public async Task<IActionResult> Mine(
        [FromQuery] string nic,
        [FromQuery] string? search = null,
        [FromQuery] string? status = null)
    {
        // Query reservation history for the authenticated prosumer
        var reservations = await service.GetMineAsync(nic, search, status);
        return Ok(reservations);
    }

    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpGet("status/{status}")]
    public async Task<IActionResult> ByStatus(string status)
    {
        // List reservations filtered by status across stations
        var list = await service.GetByStatusAsync(status);
        return Ok(list);
    }

    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpGet("all")]
    public async Task<IActionResult> GetAll()
    {
        // Retrieve all reservations across the microgrid network
        var all = await service.GetAllAsync();
        return Ok(all);
    }

    [Authorize(Roles = "Prosumer")]
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromQuery] string nic, UpdateReservationDto d)
    {
        // Reschedule or modify energy amount for an existing reservation
        var updated = await service.UpdateAsync(nic, id, d);
        return Ok(updated);
    }

    [Authorize(Roles = "Prosumer")]
    [HttpDelete("{id}")]
    public async Task<IActionResult> Cancel(string id, [FromQuery] string nic)
    {
        // Cancel a booking and release its reserved energy back to the slot
        var cancelled = await service.CancelAsync(nic, id);
        return Ok(cancelled);
    }

    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpPut("{id}/approve")]
    public async Task<IActionResult> Approve(string id)
    {
        // Confirm reservation validity and mark status as Approved
        var approved = await service.ApproveAsync(id);
        return Ok(approved);
    }
}
