// ============================================================================
// SmartSolar Microgrid System
// File: ReservationService.cs
// Purpose: Core business logic for prosumer energy slot reservations, capacity
//          booking, modification, cancellation, and operator approval.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Reservation;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

namespace SmartSolarMicrogrid.Application.Services;

public class ReservationService : IReservationService
{
    private readonly EnergyReservationRepository _res;
    private readonly ProsumerRepository _pros;
    private readonly EnergySlotRepository _slots;
    private readonly MicrogridNodeRepository _nodes;

    public ReservationService(
        EnergyReservationRepository res,
        ProsumerRepository pros,
        EnergySlotRepository slots,
        MicrogridNodeRepository nodes)
    {
        // Inject required MongoDB repositories
        _res = res;
        _pros = pros;
        _slots = slots;
        _nodes = nodes;
    }

    public async Task<EnergyReservation> CreateAsync(string nic, CreateReservationDto d)
    {
        // Validate prosumer status, slot availability, and 7-day booking window constraints
        var prosumers = await _pros.GetAllAsync();
        var p = prosumers.FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase))
            ?? throw new KeyNotFoundException("Prosumer not found.");

        if (p.AccountStatus != UserStatus.Active)
        {
            throw new UnauthorizedAccessException("Prosumer account is not active.");
        }

        var s = await _slots.GetByIdAsync(d.EnergySlotId)
            ?? throw new KeyNotFoundException("Energy slot not found.");

        var node = await _nodes.GetByIdAsync(s.NodeId)
            ?? throw new KeyNotFoundException("Node not found.");

        if (node.Status != NodeStatus.Active || s.Status != SlotStatus.Available || s.AvailableCapacityKwh <= 0)
        {
            throw new InvalidOperationException("Slot is unavailable.");
        }

        ReservationValidator.WithinBookingWindow(s.SlotDate, s.StartTime, DateTime.UtcNow, DateTime.Now);
        Validation.Positive(d.EnergyAmountKwh, "EnergyAmountKwh");

        if (d.EnergyAmountKwh > s.AvailableCapacityKwh)
        {
            throw new InvalidOperationException("Requested energy exceeds available capacity.");
        }

        var duplicate = (await _res.GetAllAsync())
            .Any(r => r.EnergySlotId == s.Id && r.Status != ReservationStatus.Cancelled);

        if (duplicate)
        {
            throw new InvalidOperationException("This slot already has an active reservation.");
        }

        var r = new EnergyReservation
        {
            Id = Guid.NewGuid().ToString(),
            ReservationCode = "RES-" + Random.Shared.Next(1000, 9999),
            ProsumerId = p.Id,
            NodeId = node.Id,
            EnergySlotId = s.Id,
            ReservationDate = s.SlotDate,
            StartTime = s.StartTime,
            EndTime = s.EndTime,
            EnergyAmountKwh = d.EnergyAmountKwh
        };

        // Deduct energy from slot and update its availability state
        s.AvailableCapacityKwh -= d.EnergyAmountKwh;
        if (s.AvailableCapacityKwh <= 0)
        {
            s.Status = SlotStatus.Reserved;
        }
        s.UpdatedAt = DateTime.UtcNow;

        await _res.InsertAsync(r);
        await _slots.ReplaceAsync(s.Id, s);
        return r;
    }

    public async Task<EnergyReservation?> GetAsync(string id)
    {
        // Retrieve single reservation by unique ID
        return await _res.GetByIdAsync(id);
    }

    public async Task<IReadOnlyList<ReservationResponseDto>> GetMineAsync(
        string nic,
        string? search = null,
        string? status = null)
    {
        // Query reservation history for the authenticated prosumer
        var pros = await _pros.GetAllAsync();
        var p = pros.FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase))
            ?? throw new KeyNotFoundException("Prosumer not found.");

        var x = (await _res.GetAllAsync()).Where(r => r.ProsumerId == p.Id);

        if (!string.IsNullOrWhiteSpace(search))
        {
            x = x.Where(r => r.ReservationCode.Contains(search, StringComparison.OrdinalIgnoreCase)
                || r.NodeId.Contains(search, StringComparison.OrdinalIgnoreCase));
        }

        if (!string.IsNullOrWhiteSpace(status) && Enum.TryParse<ReservationStatus>(status, true, out var st))
        {
            x = x.Where(r => r.Status == st);
        }

        var nodes = await _nodes.GetAllAsync();

        return x.OrderByDescending(r => r.CreatedAt).Select(r => new ReservationResponseDto
        {
            Id = r.Id,
            ReservationCode = r.ReservationCode,
            ProsumerId = r.ProsumerId,
            ProsumerName = p.FullName,
            NodeId = r.NodeId,
            NodeName = nodes.FirstOrDefault(n => n.Id == r.NodeId)?.NodeName,
            EnergySlotId = r.EnergySlotId,
            ReservationDate = r.ReservationDate,
            StartTime = r.StartTime,
            EndTime = r.EndTime,
            EnergyAmountKwh = r.EnergyAmountKwh,
            Status = r.Status,
            CreatedAt = r.CreatedAt
        }).ToList();
    }

    public async Task<IReadOnlyList<ReservationResponseDto>> GetByStatusAsync(string status)
    {
        // List reservations filtered by lifecycle status
        if (!Enum.TryParse<ReservationStatus>(status, true, out var st))
        {
            throw new ArgumentException("Invalid status.");
        }

        var res = (await _res.GetAllAsync())
            .Where(r => r.Status == st)
            .OrderBy(r => r.ReservationDate)
            .ToList();

        var pros = await _pros.GetAllAsync();
        var nodes = await _nodes.GetAllAsync();

        return res.Select(r => new ReservationResponseDto
        {
            Id = r.Id,
            ReservationCode = r.ReservationCode,
            ProsumerId = r.ProsumerId,
            ProsumerName = pros.FirstOrDefault(p => p.Id == r.ProsumerId)?.FullName,
            NodeId = r.NodeId,
            NodeName = nodes.FirstOrDefault(n => n.Id == r.NodeId)?.NodeName,
            EnergySlotId = r.EnergySlotId,
            ReservationDate = r.ReservationDate,
            StartTime = r.StartTime,
            EndTime = r.EndTime,
            EnergyAmountKwh = r.EnergyAmountKwh,
            Status = r.Status,
            CreatedAt = r.CreatedAt
        }).ToList();
    }

    public async Task<IReadOnlyList<ReservationResponseDto>> GetAllAsync()
    {
        // Retrieve master list of all reservations across nodes
        var res = (await _res.GetAllAsync())
            .OrderByDescending(r => r.ReservationDate)
            .ToList();

        var pros = await _pros.GetAllAsync();
        var nodes = await _nodes.GetAllAsync();

        return res.Select(r => new ReservationResponseDto
        {
            Id = r.Id,
            ReservationCode = r.ReservationCode,
            ProsumerId = r.ProsumerId,
            ProsumerName = pros.FirstOrDefault(p => p.Id == r.ProsumerId)?.FullName,
            NodeId = r.NodeId,
            NodeName = nodes.FirstOrDefault(n => n.Id == r.NodeId)?.NodeName,
            EnergySlotId = r.EnergySlotId,
            ReservationDate = r.ReservationDate,
            StartTime = r.StartTime,
            EndTime = r.EndTime,
            EnergyAmountKwh = r.EnergyAmountKwh,
            Status = r.Status,
            CreatedAt = r.CreatedAt
        }).ToList();
    }

    public async Task<EnergyReservation> UpdateAsync(string nic, string id, UpdateReservationDto d)
    {
        // Reschedule booking to another slot or adjust energy amount within the 7-day window
        var r = await _res.GetByIdAsync(id)
            ?? throw new KeyNotFoundException("Reservation not found.");

        var pros = await _pros.GetAllAsync();
        var p = pros.FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase))
            ?? throw new KeyNotFoundException("Prosumer not found.");

        if (r.ProsumerId != p.Id)
        {
            throw new UnauthorizedAccessException("Not your reservation.");
        }

        if (r.Status != ReservationStatus.Pending && r.Status != ReservationStatus.Approved)
        {
            throw new InvalidOperationException("Reservation cannot be modified.");
        }

        ReservationValidator.HasMinimumNotice(ReservationValidator.StartDateTime(r.ReservationDate, r.StartTime), DateTime.UtcNow, "Modification");
        Validation.Positive(d.EnergyAmountKwh, "EnergyAmountKwh");

        var s = await _slots.GetByIdAsync(d.EnergySlotId)
            ?? throw new KeyNotFoundException("Energy slot not found.");

        ReservationValidator.WithinBookingWindow(s.SlotDate, s.StartTime, DateTime.UtcNow, DateTime.Now);

        if (r.EnergySlotId != s.Id)
        {
            if (s.Status != SlotStatus.Available || s.AvailableCapacityKwh < d.EnergyAmountKwh)
            {
                throw new InvalidOperationException("New slot is unavailable.");
            }

            var oldSlot = await _slots.GetByIdAsync(r.EnergySlotId);
            if (oldSlot != null)
            {
                oldSlot.AvailableCapacityKwh += r.EnergyAmountKwh;
                if (oldSlot.Status == SlotStatus.Reserved && oldSlot.AvailableCapacityKwh > 0)
                {
                    oldSlot.Status = SlotStatus.Available;
                }
                oldSlot.UpdatedAt = DateTime.UtcNow;
                await _slots.ReplaceAsync(oldSlot.Id, oldSlot);
            }

            s.AvailableCapacityKwh -= d.EnergyAmountKwh;
            if (s.AvailableCapacityKwh <= 0)
            {
                s.Status = SlotStatus.Reserved;
            }
            s.UpdatedAt = DateTime.UtcNow;
            await _slots.ReplaceAsync(s.Id, s);
        }
        else
        {
            double diff = d.EnergyAmountKwh - r.EnergyAmountKwh;
            if (diff > 0 && s.AvailableCapacityKwh < diff)
            {
                throw new InvalidOperationException("Requested energy exceeds available capacity.");
            }
            s.AvailableCapacityKwh -= diff;
            if (s.AvailableCapacityKwh <= 0)
            {
                s.Status = SlotStatus.Reserved;
            }
            else if (s.AvailableCapacityKwh > 0 && s.Status == SlotStatus.Reserved)
            {
                s.Status = SlotStatus.Available;
            }
            s.UpdatedAt = DateTime.UtcNow;
            await _slots.ReplaceAsync(s.Id, s);
        }

        r.EnergySlotId = s.Id;
        r.NodeId = s.NodeId;
        r.ReservationDate = s.SlotDate;
        r.StartTime = s.StartTime;
        r.EndTime = s.EndTime;
        r.EnergyAmountKwh = d.EnergyAmountKwh;
        r.Status = ReservationStatus.Pending;

        await _res.ReplaceAsync(id, r);
        return r;
    }

    public async Task<EnergyReservation> CancelAsync(string nic, string id)
    {
        // Cancel reservation with notice and restore energy capacity to the slot
        var r = await _res.GetByIdAsync(id)
            ?? throw new KeyNotFoundException("Reservation not found.");

        var pros = await _pros.GetAllAsync();
        var p = pros.FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase))
            ?? throw new KeyNotFoundException("Prosumer not found.");

        if (r.ProsumerId != p.Id)
        {
            throw new UnauthorizedAccessException("Not your reservation.");
        }

        if (r.Status == ReservationStatus.Cancelled)
        {
            throw new InvalidOperationException("Already cancelled.");
        }

        ReservationValidator.HasMinimumNotice(ReservationValidator.StartDateTime(r.ReservationDate, r.StartTime), DateTime.UtcNow, "Cancellation");

        r.Status = ReservationStatus.Cancelled;
        r.CancelledAt = DateTime.UtcNow;
        await _res.ReplaceAsync(id, r);

        var s = await _slots.GetByIdAsync(r.EnergySlotId);
        if (s is not null)
        {
            s.Status = SlotStatus.Available;
            s.AvailableCapacityKwh += r.EnergyAmountKwh;
            await _slots.ReplaceAsync(s.Id, s);
        }

        return r;
    }

    public async Task<EnergyReservation> ApproveAsync(string id)
    {
        // Approve a pending reservation and record approval timestamp
        var r = await _res.GetByIdAsync(id)
            ?? throw new KeyNotFoundException("Reservation not found.");

        if (r.Status != ReservationStatus.Pending)
        {
            throw new InvalidOperationException("Only pending reservations can be approved.");
        }

        r.Status = ReservationStatus.Approved;
        r.ApprovedAt = DateTime.UtcNow;

        await _res.ReplaceAsync(id, r);
        return r;
    }
}
