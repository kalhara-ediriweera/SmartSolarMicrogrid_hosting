// ============================================================================
// SmartSolar Microgrid System
// File: SlotUnavailabilityRequest.cs
// Purpose: Domain entity representing SlotUnavailabilityRequest within the solar microgrid system.
// ============================================================================

using SmartSolarMicrogrid.Domain.Enums;
namespace SmartSolarMicrogrid.Domain.Entities;

public class SlotUnavailabilityRequest
{
    public string Id { get; set; } = "";
    public string SlotId { get; set; } = "";
    public string NodeId { get; set; } = "";
    public string RequestedByUserId { get; set; } = "";
    public string RequestedByUsername { get; set; } = "";
    public string Reason { get; set; } = "";
    public SlotRequestStatus Status { get; set; } = SlotRequestStatus.Pending;
    public string? AdminNote { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? ResolvedAt { get; set; }
}
