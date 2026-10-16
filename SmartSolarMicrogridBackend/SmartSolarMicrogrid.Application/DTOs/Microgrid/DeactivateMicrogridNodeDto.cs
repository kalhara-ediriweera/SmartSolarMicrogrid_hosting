// ============================================================================
// SmartSolar Microgrid System
// File: DeactivateMicrogridNodeDto.cs
// Purpose: Data transfer object representing DeactivateMicrogridNode payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.Microgrid;

public record DeactivateMicrogridNodeDto(string? Reason = null, string? AdminNote = null);
