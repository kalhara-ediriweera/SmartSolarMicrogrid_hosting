using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.QR;
using SmartSolarMicrogrid.Application.Interfaces;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/qr")]
public class QRTransactionController(IQRTransactionService service):ControllerBase {
    [Authorize(Roles="Prosumer"),HttpPost("generate/{reservationId}")] public async Task<IActionResult> Generate(string reservationId)=>Ok(await service.GenerateAsync(reservationId));
    [Authorize(Roles="GridOperator"),HttpPost("verify")] public async Task<IActionResult> Verify(VerifyQRDto d)=>Ok(await service.VerifyAsync(d));
    [Authorize(Roles="GridOperator"),HttpPost("complete")] public async Task<IActionResult> Complete(VerifyQRDto d)=>Ok(await service.CompleteAsync(d));
}
