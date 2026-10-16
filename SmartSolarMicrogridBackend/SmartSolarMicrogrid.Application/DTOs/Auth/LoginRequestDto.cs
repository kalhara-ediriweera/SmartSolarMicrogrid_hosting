// ============================================================================
// SmartSolar Microgrid System
// File: LoginRequestDto.cs
// Purpose: Data transfer object representing LoginRequest payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.Auth;

public record LoginRequestDto(string Username, string Password);
