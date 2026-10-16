// ============================================================================
// SmartSolar Microgrid System
// File: IOperatorService.cs
// Purpose: Service contract defining operations and abstractions for Operator.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Dashboard;
using SmartSolarMicrogrid.Application.DTOs.Operator;
using SmartSolarMicrogrid.Application.DTOs.Reservation;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IOperatorService
{
    Task<OperatorProfileDto> MeAsync(string userId);
    Task<IReadOnlyList<OperatorSlotDto>> SlotsAsync(string userId, bool availableOnly = false);
    Task<IReadOnlyList<ReservationResponseDto>> ReservationsAsync(string userId, string status);
    Task<EnergyReservation> ApproveReservationAsync(string userId, string reservationId);
    Task<OperatorDashboardDto> DashboardAsync(string userId);
    Task<SlotRequestDto> CreateSlotRequestAsync(string userId, CreateSlotRequestDto dto);
    Task<IReadOnlyList<SlotRequestDto>> MySlotRequestsAsync(string userId);
    Task<IReadOnlyList<SlotRequestDto>> SlotRequestsAsync(string? status);
    Task<SlotRequestDto> ApproveSlotRequestAsync(string id, string? adminNote);
    Task<SlotRequestDto> RejectSlotRequestAsync(string id, string? adminNote);
    Task<IReadOnlyList<OperatorAssignmentDto>> AssignmentsAsync();
    Task<OperatorAssignmentDto> AssignStationAsync(string userId, string? stationId);
}
