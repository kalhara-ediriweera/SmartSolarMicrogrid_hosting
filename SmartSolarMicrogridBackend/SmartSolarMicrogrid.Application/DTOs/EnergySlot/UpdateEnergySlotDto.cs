// ============================================================================
// SmartSolar Microgrid System
// File: UpdateEnergySlotDto.cs
// Purpose: Data transfer object representing UpdateEnergySlot payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.EnergySlot;

public record UpdateEnergySlotDto(DateTime SlotDate, string StartTime, string EndTime, double EnergyAmountKwh, double AvailableCapacityKwh, string Status);
