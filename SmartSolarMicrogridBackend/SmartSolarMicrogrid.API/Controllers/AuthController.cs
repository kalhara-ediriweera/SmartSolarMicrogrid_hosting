using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Auth;
using SmartSolarMicrogrid.Application.Interfaces;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/auth")]
public class AuthController(IAuthService service):ControllerBase {
    [AllowAnonymous,HttpPost("login")] public async Task<IActionResult> Login(LoginRequestDto dto)=>Ok(await service.LoginAsync(dto));
}
