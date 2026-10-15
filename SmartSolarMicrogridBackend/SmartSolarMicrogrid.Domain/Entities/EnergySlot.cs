using SmartSolarMicrogrid.Domain.Enums;
namespace SmartSolarMicrogrid.Domain.Entities;
public class EnergySlot {
    public string Id { get; set; } = "";
    public string NodeId { get; set; } = "";
    public DateTime SlotDate { get; set; }
    public string StartTime { get; set; } = "";
    public string EndTime { get; set; } = "";
    public double EnergyAmountKwh { get; set; }
    public double AvailableCapacityKwh { get; set; }
    public SlotStatus Status { get; set; } = SlotStatus.Available;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
