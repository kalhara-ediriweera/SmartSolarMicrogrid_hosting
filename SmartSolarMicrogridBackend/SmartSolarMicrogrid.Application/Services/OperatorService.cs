// ============================================================================
// SmartSolar Microgrid System
// File: OperatorService.cs
// Purpose: Implements branch-scoped operations for grid operators and administrative
//          branch assignment / slot unavailability approval workflows.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Dashboard;
using SmartSolarMicrogrid.Application.DTOs.Operator;
using SmartSolarMicrogrid.Application.DTOs.Reservation;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

namespace SmartSolarMicrogrid.Application.Services;

public class OperatorService : IOperatorService
{
    private readonly GridOperatorRepository _ops;
    private readonly UserRepository _users;
    private readonly MicrogridNodeRepository _nodes;
    private readonly EnergySlotRepository _slots;
    private readonly EnergyReservationRepository _res;
    private readonly SlotUnavailabilityRequestRepository _req;
    private readonly IReservationService _reservations;

    public OperatorService(
        GridOperatorRepository ops,
        UserRepository users,
        MicrogridNodeRepository nodes,
        EnergySlotRepository slots,
        EnergyReservationRepository res,
        SlotUnavailabilityRequestRepository req,
        IReservationService reservations)
    {
        // Initialize repositories and services for operator workflows
        _ops = ops;
        _users = users;
        _nodes = nodes;
        _slots = slots;
        _res = res;
        _req = req;
        _reservations = reservations;
    }

    private async Task<string?> StationIdAsync(string userId)
    {
        // Look up assigned station identifier for the operator
        var op = (await _ops.GetAllAsync()).FirstOrDefault(x => x.UserId == userId);
        return string.IsNullOrWhiteSpace(op?.StationId) ? null : op.StationId;
    }

    private async Task<string> RequireStationIdAsync(string userId)
    {
        // Enforce that the operator is assigned to a valid station branch
        return await StationIdAsync(userId)
            ?? throw new InvalidOperationException("No branch is assigned to this grid operator. Ask the Backoffice admin to assign one.");
    }

    public async Task<OperatorProfileDto> MeAsync(string userId)
    {
        // Retrieve current operator details alongside their assigned station node
        var user = await _users.GetByIdAsync(userId)
            ?? throw new KeyNotFoundException("User not found.");

        var stationId = await StationIdAsync(userId);
        var node = stationId is null ? null : await _nodes.GetByIdAsync(stationId);

        return new OperatorProfileDto(
            user.Id,
            user.Username,
            stationId,
            node?.NodeCode,
            node?.NodeName,
            node?.Status.ToString()
        );
    }

    public async Task<IReadOnlyList<OperatorSlotDto>> SlotsAsync(string userId, bool availableOnly = false)
    {
        // Return energy slots scoped exclusively to this operator's assigned branch
        var stationId = await StationIdAsync(userId);
        if (stationId is null)
        {
            return new List<OperatorSlotDto>();
        }

        var node = await _nodes.GetByIdAsync(stationId);
        var query = (await _slots.GetAllAsync()).Where(s => s.NodeId == stationId).ToList();

        foreach (var s in query)
        {
            if (s.AvailableCapacityKwh > 0 && s.Status == SlotStatus.Reserved)
            {
                s.Status = SlotStatus.Available;
            }
            else if (s.AvailableCapacityKwh <= 0 && s.Status == SlotStatus.Available)
            {
                s.Status = SlotStatus.Reserved;
            }
        }

        if (availableOnly)
        {
            query = query.Where(s => s.Status == SlotStatus.Available && s.AvailableCapacityKwh > 0).ToList();
        }

        var slots = query.OrderBy(s => s.SlotDate).ThenBy(s => s.StartTime).ToList();
        var requests = (await _req.GetAllAsync()).Where(r => r.NodeId == stationId).ToList();

        return slots.Select(s =>
        {
            string? reason = null;
            if (s.Status == SlotStatus.Unavailable)
            {
                reason = requests
                    .Where(r => r.SlotId == s.Id && r.Status == SlotRequestStatus.Approved)
                    .OrderByDescending(r => r.ResolvedAt)
                    .FirstOrDefault()?.Reason;

                if (reason is null && node is not null && node.Status != NodeStatus.Active)
                {
                    reason = node.AdminNote ?? node.DeactivationReason ?? "Branch deactivated";
                }
            }
            var pending = requests.Any(r => r.SlotId == s.Id && r.Status == SlotRequestStatus.Pending);
            return new OperatorSlotDto(
                s.Id,
                s.NodeId,
                s.SlotDate,
                s.StartTime,
                s.EndTime,
                s.EnergyAmountKwh,
                s.AvailableCapacityKwh,
                s.Status.ToString(),
                reason,
                pending
            );
        }).ToList();
    }

    public async Task<IReadOnlyList<ReservationResponseDto>> ReservationsAsync(string userId, string status)
    {
        // Query branch-scoped reservations by status
        var stationId = await StationIdAsync(userId);
        if (stationId is null)
        {
            return new List<ReservationResponseDto>();
        }

        if (string.IsNullOrWhiteSpace(status) || "all".Equals(status, StringComparison.OrdinalIgnoreCase))
        {
            return (await _reservations.GetAllAsync()).Where(r => r.NodeId == stationId).ToList();
        }

        return (await _reservations.GetByStatusAsync(status)).Where(r => r.NodeId == stationId).ToList();
    }

    public async Task<EnergyReservation> ApproveReservationAsync(string userId, string reservationId)
    {
        // Validate branch ownership before approving a prosumer reservation
        var stationId = await RequireStationIdAsync(userId);
        var r = await _res.GetByIdAsync(reservationId)
            ?? throw new KeyNotFoundException("Reservation not found.");

        if (r.NodeId != stationId)
        {
            throw new UnauthorizedAccessException("This reservation belongs to another branch.");
        }

        return await _reservations.ApproveAsync(reservationId);
    }

    public async Task<OperatorDashboardDto> DashboardAsync(string userId)
    {
        // Calculate operational summary counters for the operator's branch
        var stationId = await StationIdAsync(userId);
        if (stationId is null)
        {
            return new OperatorDashboardDto(0, 0, 0, 0, new List<object>());
        }

        var r = (await _res.GetAllAsync()).Where(x => x.NodeId == stationId).ToList();
        var s = (await _slots.GetAllAsync()).Where(x => x.NodeId == stationId).ToList();

        return new OperatorDashboardDto(
            r.Count(x => x.Status == ReservationStatus.Pending),
            r.Count(x => x.Status == ReservationStatus.Approved),
            r.Count(x => x.ReservationDate.Date == DateTime.UtcNow.Date),
            s.Count(x => x.Status == SlotStatus.Available),
            new List<object>()
        );
    }

    public async Task<SlotRequestDto> CreateSlotRequestAsync(string userId, CreateSlotRequestDto dto)
    {
        // Submit an unavailability request for a slot belonging to operator's station
        if (string.IsNullOrWhiteSpace(dto.Reason))
        {
            throw new ArgumentException("A reason is required.");
        }

        var stationId = await RequireStationIdAsync(userId);
        var slot = await _slots.GetByIdAsync(dto.SlotId)
            ?? throw new KeyNotFoundException("Slot not found.");

        if (slot.NodeId != stationId)
        {
            throw new UnauthorizedAccessException("This slot belongs to another branch.");
        }

        if (slot.Status != SlotStatus.Available)
        {
            throw new InvalidOperationException("Only available slots can be requested to be made unavailable.");
        }

        var hasPending = (await _req.GetAllAsync()).Any(r => r.SlotId == slot.Id && r.Status == SlotRequestStatus.Pending);
        if (hasPending)
        {
            throw new InvalidOperationException("A request for this slot is already pending.");
        }

        var user = await _users.GetByIdAsync(userId);
        var req = new SlotUnavailabilityRequest
        {
            Id = Guid.NewGuid().ToString(),
            SlotId = slot.Id,
            NodeId = slot.NodeId,
            RequestedByUserId = userId,
            RequestedByUsername = user?.Username ?? "",
            Reason = dto.Reason.Trim()
        };

        await _req.InsertAsync(req);
        return await ToDtoAsync(req);
    }

    public async Task<IReadOnlyList<SlotRequestDto>> MySlotRequestsAsync(string userId)
    {
        // List unavailability requests filed for the operator's station
        var stationId = await StationIdAsync(userId);
        if (stationId is null)
        {
            return new List<SlotRequestDto>();
        }

        var requests = (await _req.GetAllAsync()).Where(r => r.NodeId == stationId);
        return await ToDtosAsync(requests);
    }

    public async Task<IReadOnlyList<SlotRequestDto>> SlotRequestsAsync(string? status)
    {
        // Retrieve all slot requests across stations for backoffice review
        var all = (await _req.GetAllAsync()).AsEnumerable();
        if (!string.IsNullOrWhiteSpace(status))
        {
            if (!Enum.TryParse<SlotRequestStatus>(status, true, out var st))
            {
                throw new ArgumentException("Invalid status.");
            }
            all = all.Where(r => r.Status == st);
        }

        return await ToDtosAsync(all);
    }

    public async Task<SlotRequestDto> ApproveSlotRequestAsync(string id, string? adminNote)
    {
        // Backoffice approval: lock energy slot and mark request approved
        var req = await PendingRequestAsync(id);
        var slot = await _slots.GetByIdAsync(req.SlotId)
            ?? throw new KeyNotFoundException("Slot not found.");

        slot.Status = SlotStatus.Unavailable;
        slot.UpdatedAt = DateTime.UtcNow;

        await _slots.ReplaceAsync(slot.Id, slot);
        return await ResolveAsync(req, SlotRequestStatus.Approved, adminNote);
    }

    public async Task<SlotRequestDto> RejectSlotRequestAsync(string id, string? adminNote)
    {
        // Backoffice rejection: reject maintenance request with admin reasoning
        var req = await PendingRequestAsync(id);
        return await ResolveAsync(req, SlotRequestStatus.Rejected, adminNote);
    }

    public async Task<IReadOnlyList<OperatorAssignmentDto>> AssignmentsAsync()
    {
        // Query grid operators and their mapped microgrid stations
        var users = (await _users.GetAllAsync())
            .Where(u => u.Role == UserRole.GridOperator)
            .OrderBy(u => u.Username)
            .ToList();

        var ops = await _ops.GetAllAsync();
        var nodes = await _nodes.GetAllAsync();

        return users.Select(u =>
        {
            var sid = ops.FirstOrDefault(o => o.UserId == u.Id)?.StationId;
            var n = nodes.FirstOrDefault(x => x.Id == sid);
            return new OperatorAssignmentDto(
                u.Id,
                u.Username,
                string.IsNullOrWhiteSpace(sid) ? null : sid,
                n?.NodeCode,
                n?.NodeName
            );
        }).ToList();
    }

    public async Task<OperatorAssignmentDto> AssignStationAsync(string userId, string? stationId)
    {
        // Link or reassign a grid operator user account to a designated microgrid station
        var user = await _users.GetByIdAsync(userId)
            ?? throw new KeyNotFoundException("User not found.");

        if (user.Role != UserRole.GridOperator)
        {
            throw new ArgumentException("Only grid operators can be assigned to a branch.");
        }

        MicrogridNode? node = null;
        if (!string.IsNullOrWhiteSpace(stationId))
        {
            node = await _nodes.GetByIdAsync(stationId)
                ?? throw new KeyNotFoundException("Branch not found.");
        }

        var op = (await _ops.GetAllAsync()).FirstOrDefault(o => o.UserId == userId);
        if (op is null)
        {
            op = new GridOperator
            {
                Id = Guid.NewGuid().ToString(),
                UserId = userId,
                FullName = user.Username,
                StationId = node?.Id ?? ""
            };
            await _ops.InsertAsync(op);
        }
        else
        {
            op.StationId = node?.Id ?? "";
            await _ops.ReplaceAsync(op.Id, op);
        }

        return new OperatorAssignmentDto(user.Id, user.Username, node?.Id, node?.NodeCode, node?.NodeName);
    }

    private async Task<SlotUnavailabilityRequest> PendingRequestAsync(string id)
    {
        // Ensure request exists and has not yet been resolved
        var req = await _req.GetByIdAsync(id)
            ?? throw new KeyNotFoundException("Request not found.");

        if (req.Status != SlotRequestStatus.Pending)
        {
            throw new InvalidOperationException("This request has already been resolved.");
        }
        return req;
    }

    private async Task<SlotRequestDto> ResolveAsync(SlotUnavailabilityRequest req, SlotRequestStatus status, string? adminNote)
    {
        // Update request resolution timestamp, admin note, and resolved status
        req.Status = status;
        req.AdminNote = string.IsNullOrWhiteSpace(adminNote) ? null : adminNote.Trim();
        req.ResolvedAt = DateTime.UtcNow;

        await _req.ReplaceAsync(req.Id, req);
        return await ToDtoAsync(req);
    }

    private async Task<SlotRequestDto> ToDtoAsync(SlotUnavailabilityRequest r)
    {
        // Convert single request entity to DTO representation
        return (await ToDtosAsync(new[] { r }))[0];
    }

    private async Task<IReadOnlyList<SlotRequestDto>> ToDtosAsync(IEnumerable<SlotUnavailabilityRequest> reqs)
    {
        // Project request entities with enriched node and slot metadata
        var nodes = await _nodes.GetAllAsync();
        var slots = await _slots.GetAllAsync();

        return reqs.OrderByDescending(r => r.CreatedAt).Select(r =>
        {
            var n = nodes.FirstOrDefault(x => x.Id == r.NodeId);
            var s = slots.FirstOrDefault(x => x.Id == r.SlotId);
            return new SlotRequestDto(
                r.Id,
                r.SlotId,
                r.NodeId,
                n?.NodeCode,
                n?.NodeName,
                s?.SlotDate,
                s?.StartTime,
                s?.EndTime,
                r.RequestedByUsername,
                r.Reason,
                r.Status.ToString(),
                r.AdminNote,
                r.CreatedAt,
                r.ResolvedAt
            );
        }).ToList();
    }
}
