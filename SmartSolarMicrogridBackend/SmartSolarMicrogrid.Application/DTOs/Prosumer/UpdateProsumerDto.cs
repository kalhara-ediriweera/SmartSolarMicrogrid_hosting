// ============================================================================
// SmartSolar Microgrid System
// File: UpdateProsumerDto.cs
// Purpose: Data transfer object representing UpdateProsumer payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.Prosumer;

public record UpdateProsumerDto(string FullName, string Email, string Phone, string Address);
