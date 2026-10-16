// ============================================================================
// SmartSolar Microgrid System
// File: IAuthService.cs
// Purpose: Contract for user authentication, password verification, and JWT
//          token issuance across all system roles.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Auth;

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IAuthService
{
    Task<LoginResponseDto> LoginAsync(LoginRequestDto request);
}