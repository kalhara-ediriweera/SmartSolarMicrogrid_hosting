// ============================================================================
// SmartSolar Microgrid System
// File: UpdateMicrogridNodeDto.cs
// Purpose: Data transfer object representing UpdateMicrogridNode payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.Microgrid;

public record UpdateMicrogridNodeDto(string NodeName, double Latitude, double Longitude, double CapacityKw, int BatterySlotAvailability, string ScheduleStart, string ScheduleEnd);
