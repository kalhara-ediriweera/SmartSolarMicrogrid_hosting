// ============================================================================
// SmartSolar Microgrid System
// File: LoginResponseDto.cs
// Purpose: Data transfer object representing LoginResponse payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.Auth;

public record LoginResponseDto(string Token, string UserId, string Username, string Role, string Status, string Nic = "");
