using SmartSolarMicrogrid.Domain.Enums;
namespace SmartSolarMicrogrid.Domain.Entities;
public class EnergyReservation {
    public string Id { get; set; } = "";
    public string ReservationCode { get; set; } = "";
    public string ProsumerId { get; set; } = "";
    public string NodeId { get; set; } = "";
    public string EnergySlotId { get; set; } = "";
    public DateTime ReservationDate { get; set; }
    public string StartTime { get; set; } = "";
    public string EndTime { get; set; } = "";
    public double EnergyAmountKwh { get; set; }
    public ReservationStatus Status { get; set; } = ReservationStatus.Pending;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? ApprovedAt { get; set; }
    public DateTime? CancelledAt { get; set; }
    public DateTime? CompletedAt { get; set; }
}
