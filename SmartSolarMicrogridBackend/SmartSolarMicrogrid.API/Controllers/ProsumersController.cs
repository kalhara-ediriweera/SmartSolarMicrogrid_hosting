// ============================================================================
// SmartSolar Microgrid System
// File: ProsumersController.cs
// Purpose: REST API endpoints for prosumer onboarding, verification, profile
//          management, and administrative account lifecycle control.
// ============================================================================

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Prosumer;
using SmartSolarMicrogrid.Application.Interfaces;

namespace SmartSolarMicrogrid.API.Controllers;

[ApiController]
[Route("api/prosumers")]
public class ProsumersController(IProsumerService service) : ControllerBase
{
    [AllowAnonymous]
    [HttpPost("register")]
    public async Task<IActionResult> Register(CreateProsumerDto d)
    {
        // Register a new solar prosumer account with initial pending status
        var prosumer = await service.RegisterAsync(d);
        return Ok(prosumer);
    }

    [Authorize]
    [HttpGet("{nic}")]
    public async Task<IActionResult> Get(string nic)
    {
        // Retrieve profile details for the given National Identity Card number
        var prosumer = await service.GetByNicAsync(nic);
        return Ok(prosumer);
    }

    [Authorize(Roles = "Prosumer")]
    [HttpPut("{nic}")]
    public async Task<IActionResult> Update(string nic, UpdateProsumerDto d)
    {
        // Update contact information, address, and solar installation metadata
        var updated = await service.UpdateAsync(nic, d);
        return Ok(updated);
    }

    [Authorize(Roles = "Prosumer")]
    [HttpPost("{nic}/deactivation-request")]
    public async Task<IActionResult> DeactivateRequest(string nic)
    {
        // Submit account deactivation request for administrative review
        var result = await service.RequestDeactivationAsync(nic);
        return Ok(result);
    }

    [Authorize(Roles = "Backoffice")]
    [HttpGet("pending")]
    public async Task<IActionResult> Pending()
    {
        // List all newly registered prosumers awaiting NIC / DRP verification
        var pendingList = await service.GetPendingAsync();
        return Ok(pendingList);
    }

    [Authorize(Roles = "Backoffice")]
    [HttpGet("all")]
    public async Task<IActionResult> All()
    {
        // Retrieve master list of all registered prosumers across statuses
        var allProsumers = await service.GetAllAsync();
        return Ok(allProsumers);
    }

    [Authorize(Roles = "Backoffice")]
    [HttpPut("{nic}/activate")]
    public async Task<IActionResult> Activate(string nic)
    {
        // Approve and activate prosumer account after document validation
        var activated = await service.SetStatusAsync(nic, "activate");
        return Ok(activated);
    }

    [Authorize(Roles = "Backoffice")]
    [HttpPut("{nic}/verify-nic")]
    public async Task<IActionResult> VerifyNic(string nic)
    {
        // Mark national identity card as officially verified by backoffice
        var verified = await service.SetStatusAsync(nic, "verify-nic");
        return Ok(verified);
    }

    [Authorize(Roles = "Backoffice")]
    [HttpPut("{nic}/deactivate")]
    public async Task<IActionResult> Deactivate(string nic)
    {
        // Deactivate an active prosumer account due to policy or user request
        var deactivated = await service.SetStatusAsync(nic, "deactivate");
        return Ok(deactivated);
    }

    [Authorize(Roles = "Backoffice")]
    [HttpPut("{nic}/reactivate")]
    public async Task<IActionResult> Reactivate(string nic)
    {
        // Re-enable access for a previously suspended or deactivated prosumer
        var reactivated = await service.SetStatusAsync(nic, "reactivate");
        return Ok(reactivated);
    }

    [Authorize(Roles = "Backoffice")]
    [HttpPut("{nic}/reject")]
    public async Task<IActionResult> Reject(string nic)
    {
        // Reject a pending prosumer registration if eligibility checks fail
        var rejected = await service.SetStatusAsync(nic, "reject");
        return Ok(rejected);
    }
}
