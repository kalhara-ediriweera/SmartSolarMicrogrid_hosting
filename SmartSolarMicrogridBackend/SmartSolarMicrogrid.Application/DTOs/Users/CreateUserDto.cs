using System.ComponentModel.DataAnnotations;
using SmartSolarMicrogrid.Domain.Enums;

namespace SmartSolarMicrogrid.Application.DTOs.Users;

public class CreateUserDto
{
    [Required]
    public string Username { get; set; } = string.Empty;

    [Required]
    public string Password { get; set; } = string.Empty;

    [Required]
    public UserRole Role { get; set; }

    public UserStatus Status { get; set; } = UserStatus.Active;
}
