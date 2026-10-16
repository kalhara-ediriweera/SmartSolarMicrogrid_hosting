// ============================================================================
// SmartSolar Microgrid System
// File: QRTransactionController.cs
// Purpose: API endpoints for generating, validating, and completing secure QR
//          energy transfer transactions between prosumers and grid stations.
// ============================================================================

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.QR;
using SmartSolarMicrogrid.Application.Interfaces;

namespace SmartSolarMicrogrid.API.Controllers;

[ApiController]
[Route("api/qr")]
public class QRTransactionController(IQRTransactionService service) : ControllerBase
{
    [Authorize(Roles = "Prosumer")]
    [HttpPost("generate/{reservationId}")]
    public async Task<IActionResult> Generate(string reservationId)
    {
        // Generate an encrypted QR token for a confirmed prosumer reservation
        var transaction = await service.GenerateAsync(reservationId);
        return Ok(transaction);
    }

    [Authorize(Roles = "GridOperator")]
    [HttpPost("verify")]
    public async Task<IActionResult> Verify(VerifyQRDto d)
    {
        // Validate scanned QR token integrity, expiry, and reservation details
        var verificationResult = await service.VerifyAsync(d);
        return Ok(verificationResult);
    }

    [Authorize(Roles = "GridOperator")]
    [HttpPost("complete")]
    public async Task<IActionResult> Complete(VerifyQRDto d)
    {
        // Finalize transaction and discharge the reserved energy allocation
        var completionResult = await service.CompleteAsync(d);
        return Ok(completionResult);
    }
}
