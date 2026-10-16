// ============================================================================
// SmartSolar Microgrid System
// File: UpdateReservationDto.cs
// Purpose: Data transfer object representing UpdateReservation payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.Reservation;

public record UpdateReservationDto(string EnergySlotId, double EnergyAmountKwh);
