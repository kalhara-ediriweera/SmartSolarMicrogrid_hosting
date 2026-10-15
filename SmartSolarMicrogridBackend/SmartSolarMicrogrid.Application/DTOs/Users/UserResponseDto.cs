using SmartSolarMicrogrid.Domain.Enums;

namespace SmartSolarMicrogrid.Application.DTOs.Users;

public class UserResponseDto
{
    public string Id { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public UserRole Role { get; set; }
    public UserStatus Status { get; set; }
    public DateTime CreatedAt { get; set; }
}
