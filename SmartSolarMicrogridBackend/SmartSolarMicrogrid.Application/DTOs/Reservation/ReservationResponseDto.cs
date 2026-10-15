using SmartSolarMicrogrid.Domain.Enums;
namespace SmartSolarMicrogrid.Application.DTOs.Reservation;

public class ReservationResponseDto
{
    public string Id { get; set; } = "";
    public string ReservationCode { get; set; } = "";
    public string ProsumerId { get; set; } = "";
    public string? ProsumerName { get; set; }
    public string NodeId { get; set; } = "";
    public string? NodeName { get; set; }
    public string EnergySlotId { get; set; } = "";
    public DateTime ReservationDate { get; set; }
    public string StartTime { get; set; } = "";
    public string EndTime { get; set; } = "";
    public double EnergyAmountKwh { get; set; }
    public ReservationStatus Status { get; set; }
    public DateTime CreatedAt { get; set; }
}

