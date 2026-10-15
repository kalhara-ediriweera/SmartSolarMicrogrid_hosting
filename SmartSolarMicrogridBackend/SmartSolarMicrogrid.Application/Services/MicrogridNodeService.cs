using SmartSolarMicrogrid.Application.DTOs.Microgrid;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

namespace SmartSolarMicrogrid.Application.Services;

public class MicrogridNodeService : IMicrogridNodeService {
    private readonly MicrogridNodeRepository _nodes;
    private readonly EnergyReservationRepository _reservations;
    private readonly EnergySlotRepository _slots;

    public MicrogridNodeService(MicrogridNodeRepository nodes, EnergyReservationRepository reservations, EnergySlotRepository slots) {
        _nodes = nodes;
        _reservations = reservations;
        _slots = slots;
    }

    public async Task<MicrogridNode> CreateAsync(CreateMicrogridNodeDto d) {
        Validation.Required(d.NodeCode, "NodeCode");
        Validation.Required(d.NodeName, "NodeName");
        Validation.Positive(d.CapacityKw, "CapacityKw");
        Validation.LatitudeLongitude(d.Latitude, d.Longitude);
        if ((await _nodes.GetAllAsync()).Any(x => x.NodeCode.Equals(d.NodeCode, StringComparison.OrdinalIgnoreCase)))
            throw new InvalidOperationException("Node code already exists.");
        var n = new MicrogridNode {
            Id = Guid.NewGuid().ToString(),
            NodeCode = d.NodeCode,
            NodeName = d.NodeName,
            Latitude = d.Latitude,
            Longitude = d.Longitude,
            CapacityKw = d.CapacityKw,
            BatterySlotAvailability = d.BatterySlotAvailability,
            ScheduleStart = d.ScheduleStart,
            ScheduleEnd = d.ScheduleEnd
        };
        await _nodes.InsertAsync(n);
        return n;
    }

    public async Task<IReadOnlyList<MicrogridNode>> GetAllAsync(bool activeOnly = false) {
        var x = await _nodes.GetAllAsync();
        return activeOnly ? x.Where(n => n.Status == NodeStatus.Active).ToList() : x;
    }

    public Task<MicrogridNode?> GetAsync(string id) => _nodes.GetByIdAsync(id);

    public async Task<MicrogridNode> UpdateAsync(string id, UpdateMicrogridNodeDto d) {
        var n = await _nodes.GetByIdAsync(id) ?? throw new KeyNotFoundException("Node not found.");
        Validation.Positive(d.CapacityKw, "CapacityKw");
        Validation.LatitudeLongitude(d.Latitude, d.Longitude);
        n.NodeName = d.NodeName;
        n.Latitude = d.Latitude;
        n.Longitude = d.Longitude;
        n.CapacityKw = d.CapacityKw;
        n.BatterySlotAvailability = d.BatterySlotAvailability;
        n.ScheduleStart = d.ScheduleStart;
        n.ScheduleEnd = d.ScheduleEnd;
        n.UpdatedAt = DateTime.UtcNow;
        await _nodes.ReplaceAsync(id, n);
        return n;
    }

    public async Task<MicrogridNode> DeactivateAsync(string id, string? reason = null) {
        var n = await _nodes.GetByIdAsync(id) ?? throw new KeyNotFoundException("Node not found.");
        if (string.IsNullOrWhiteSpace(reason))
            throw new InvalidOperationException("Admin note / reason is required for deactivation.");

        var active = (await _reservations.GetAllAsync()).Any(r => r.NodeId == id && (r.Status == ReservationStatus.Pending || r.Status == ReservationStatus.Approved));
        if (active) throw new InvalidOperationException("Cannot deactivate node with active energy reservations.");

        n.Status = NodeStatus.Inactive;
        n.DeactivationReason = reason.Trim();
        n.AdminNote = reason.Trim();
        n.UpdatedAt = DateTime.UtcNow;
        await _nodes.ReplaceAsync(id, n);

        // Lock all existing available slots for this node to Unavailable
        var slots = (await _slots.GetAllAsync()).Where(s => s.NodeId == id && s.Status == SlotStatus.Available).ToList();
        foreach (var s in slots) {
            s.Status = SlotStatus.Unavailable;
            s.UpdatedAt = DateTime.UtcNow;
            await _slots.ReplaceAsync(s.Id, s);
        }

        return n;
    }

    public async Task<MicrogridNode> ActivateAsync(string id) {
        var n = await _nodes.GetByIdAsync(id) ?? throw new KeyNotFoundException("Node not found.");
        n.Status = NodeStatus.Active;
        n.DeactivationReason = null;
        n.AdminNote = null;
        n.UpdatedAt = DateTime.UtcNow;
        await _nodes.ReplaceAsync(id, n);
        return n;
    }

    public async Task<MicrogridNode> UpdateScheduleAsync(string id, string start, string end) {
        var n = await _nodes.GetByIdAsync(id) ?? throw new KeyNotFoundException("Node not found.");
        n.ScheduleStart = start;
        n.ScheduleEnd = end;
        n.UpdatedAt = DateTime.UtcNow;
        await _nodes.ReplaceAsync(id, n);
        return n;
    }
}
