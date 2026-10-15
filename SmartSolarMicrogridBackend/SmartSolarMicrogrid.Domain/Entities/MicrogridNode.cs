using SmartSolarMicrogrid.Domain.Enums;
namespace SmartSolarMicrogrid.Domain.Entities;
public class MicrogridNode {
    public string? DeactivationReason { get; set; }
    public string Id { get; set; } = "";
    public string NodeCode { get; set; } = "";
    public string NodeName { get; set; } = "";
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public double CapacityKw { get; set; }
    public int BatterySlotAvailability { get; set; }
    public string ScheduleStart { get; set; } = "08:00";
    public string ScheduleEnd { get; set; } = "18:00";
    public NodeStatus Status { get; set; } = NodeStatus.Active;
    public string? AdminNote { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
