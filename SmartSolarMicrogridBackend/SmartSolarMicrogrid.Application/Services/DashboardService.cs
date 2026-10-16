// ============================================================================
// SmartSolar Microgrid System
// File: DashboardService.cs
// Purpose: Aggregates real-time metrics and operational counts for prosumers,
//          station operators, and backoffice administrators.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Dashboard;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

namespace SmartSolarMicrogrid.Application.Services;

public class DashboardService : IDashboardService
{
    private readonly ProsumerRepository _prosumerRepository;
    private readonly EnergyReservationRepository _reservationRepository;
    private readonly MicrogridNodeRepository _nodeRepository;
    private readonly EnergySlotRepository _slotRepository;

    public DashboardService(
        ProsumerRepository prosumerRepository,
        EnergyReservationRepository reservationRepository,
        MicrogridNodeRepository nodeRepository,
        EnergySlotRepository slotRepository)
    {
        _prosumerRepository = prosumerRepository;
        _reservationRepository = reservationRepository;
        _nodeRepository = nodeRepository;
        _slotRepository = slotRepository;
    }

    public async Task<ProsumerDashboardDto> ProsumerAsync(string nic)
    {
        // Fetch prosumer by NIC and summarize their bookings and upcoming reservations
        var prosumers = await _prosumerRepository.GetAllAsync();
        var prosumer = prosumers.FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase))
            ?? throw new KeyNotFoundException("Prosumer not found.");

        var allReservations = await _reservationRepository.GetAllAsync();
        var prosumerReservations = allReservations.Where(x => x.ProsumerId == prosumer.Id).ToList();
        var nodes = await _nodeRepository.GetAllAsync();

        var upcoming = prosumerReservations
            .Where(x => x.Status == ReservationStatus.Approved)
            .OrderBy(x => x.ReservationDate)
            .FirstOrDefault();

        return new ProsumerDashboardDto(
            prosumerReservations.Count(x => x.Status == ReservationStatus.Approved),
            prosumerReservations.Count(x => x.Status == ReservationStatus.Pending),
            prosumerReservations.Count(x => x.Status == ReservationStatus.Completed),
            upcoming,
            nodes.Cast<object>().ToList()
        );
    }

    public async Task<OperatorDashboardDto> OperatorAsync()
    {
        // Calculate operational stats for pending bookings, approvals, and today's schedule
        var reservations = await _reservationRepository.GetAllAsync();
        var slots = await _slotRepository.GetAllAsync();

        return new OperatorDashboardDto(
            reservations.Count(x => x.Status == ReservationStatus.Pending),
            reservations.Count(x => x.Status == ReservationStatus.Approved),
            reservations.Count(x => x.ReservationDate.Date == DateTime.UtcNow.Date),
            slots.Count(x => x.Status == SlotStatus.Available),
            new List<object>()
        );
    }

    public async Task<BackofficeDashboardDto> BackofficeAsync()
    {
        // Aggregate platform-wide system totals across all nodes, slots, and prosumers
        var prosumers = await _prosumerRepository.GetAllAsync();
        var nodes = await _nodeRepository.GetAllAsync();
        var slots = await _slotRepository.GetAllAsync();
        var reservations = await _reservationRepository.GetAllAsync();

        return new BackofficeDashboardDto(
            prosumers.Count(x => x.AccountStatus == UserStatus.Active),
            prosumers.Count(x => x.AccountStatus == UserStatus.Pending),
            nodes.Count,
            slots.Count(x => x.Status == SlotStatus.Available),
            reservations.Count(x => x.Status == ReservationStatus.Pending),
            reservations.Count(x => x.Status == ReservationStatus.Approved),
            reservations.Count(x => x.Status == ReservationStatus.Completed)
        );
    }
}
