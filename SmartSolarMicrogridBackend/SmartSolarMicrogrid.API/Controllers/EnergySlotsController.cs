// ============================================================================
// SmartSolar Microgrid System
// File: EnergySlotsController.cs
// Purpose: API endpoints for managing energy slot generation, time windows,
//          and live capacity availability across microgrid stations.
// ============================================================================

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.EnergySlot;
using SmartSolarMicrogrid.Application.Interfaces;

namespace SmartSolarMicrogrid.API.Controllers;

[ApiController]
[Route("api/energy-slots")]
public class EnergySlotsController(IEnergySlotService service) : ControllerBase
{
    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpPost]
    public async Task<IActionResult> Create(CreateEnergySlotDto d)
    {
        // Provision a new scheduled energy slot for the station
        var slot = await service.CreateAsync(d);
        return Ok(slot);
    }

    [Authorize]
    [HttpGet]
    public async Task<IActionResult> Get([FromQuery] string? nodeId = null, [FromQuery] bool availableOnly = false)
    {
        // Retrieve slots filtered by station node and capacity status
        var slots = await service.GetAsync(nodeId, availableOnly);
        return Ok(slots);
    }

    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, UpdateEnergySlotDto d)
    {
        // Update slot operational window or total allocated energy
        var updated = await service.UpdateAsync(id, d);
        return Ok(updated);
    }

    [Authorize(Roles = "GridOperator,Backoffice")]
    [HttpPut("{id}/availability")]
    public async Task<IActionResult> Availability(string id, [FromQuery] double availableCapacityKwh)
    {
        // Manually adjust available capacity for an active energy slot
        var adjusted = await service.UpdateAvailabilityAsync(id, availableCapacityKwh);
        return Ok(adjusted);
    }
}
