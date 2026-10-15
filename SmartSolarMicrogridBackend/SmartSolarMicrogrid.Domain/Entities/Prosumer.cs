using SmartSolarMicrogrid.Domain.Enums;
namespace SmartSolarMicrogrid.Domain.Entities;
public class Prosumer {
    public string Id { get; set; } = "";
    public string NIC { get; set; } = "";
    public string FullName { get; set; } = "";
    public string Email { get; set; } = "";
    public string Phone { get; set; } = "";
    public string Address { get; set; } = "";
    public string UserId { get; set; } = "";
    public bool IsDrpVerified { get; set; } = false;
    public UserStatus AccountStatus { get; set; } = UserStatus.Pending;
    public string DrpVerificationRef { get; set; } = "";
    public string? ExtractedNicNumber { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    public string NicFrontImageBase64 { get; set; } = "";
    public string NicBackImageBase64 { get; set; } = "";
}
