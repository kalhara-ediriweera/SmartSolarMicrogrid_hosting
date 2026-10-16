// ============================================================================
// SmartSolar Microgrid System
// File: CreateReservationDto.cs
// Purpose: Data transfer object representing CreateReservation payload.
// ============================================================================

namespace SmartSolarMicrogrid.Application.DTOs.Reservation;

public record CreateReservationDto(string EnergySlotId, double EnergyAmountKwh);
