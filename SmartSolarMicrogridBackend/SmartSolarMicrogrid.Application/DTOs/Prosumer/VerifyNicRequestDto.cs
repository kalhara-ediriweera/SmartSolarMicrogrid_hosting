// ============================================================================
// SmartSolar Microgrid System
// File: VerifyNicRequestDto.cs
// Purpose: Data transfer object representing VerifyNicRequest payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.Prosumer;

public record VerifyNicRequestDto(string NIC, string? FullName);
