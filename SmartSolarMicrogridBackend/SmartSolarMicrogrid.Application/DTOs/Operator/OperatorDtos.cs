namespace SmartSolarMicrogrid.Application.DTOs.Operator;
public record OperatorProfileDto(string UserId, string Username, string? StationId, string? NodeCode, string? NodeName, string? NodeStatus);
public record OperatorSlotDto(string Id, string NodeId, DateTime SlotDate, string StartTime, string EndTime, double EnergyAmountKwh, double AvailableCapacityKwh, string Status, string? UnavailableReason, bool HasPendingRequest);
public record SlotRequestDto(string Id, string SlotId, string NodeId, string? NodeCode, string? NodeName, DateTime? SlotDate, string? StartTime, string? EndTime, string RequestedByUsername, string Reason, string Status, string? AdminNote, DateTime CreatedAt, DateTime? ResolvedAt);
public record OperatorAssignmentDto(string UserId, string Username, string? StationId, string? NodeCode, string? NodeName);
public record CreateSlotRequestDto(string SlotId, string Reason);
public record ResolveSlotRequestDto(string? AdminNote);
public record AssignStationDto(string? StationId);
