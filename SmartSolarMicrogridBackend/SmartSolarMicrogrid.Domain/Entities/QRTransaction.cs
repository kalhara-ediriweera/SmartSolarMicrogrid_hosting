using SmartSolarMicrogrid.Domain.Enums;
namespace SmartSolarMicrogrid.Domain.Entities;
public class QRTransaction {
    public string Id { get; set; } = "";
    public string ReservationId { get; set; } = "";
    public string TransactionCode { get; set; } = "";
    public string QRToken { get; set; } = "";
    public QRStatus Status { get; set; } = QRStatus.Generated;
    public DateTime GeneratedAt { get; set; } = DateTime.UtcNow;
    public DateTime? ScannedAt { get; set; }
    public DateTime? VerifiedAt { get; set; }
    public DateTime? CompletedAt { get; set; }
}
