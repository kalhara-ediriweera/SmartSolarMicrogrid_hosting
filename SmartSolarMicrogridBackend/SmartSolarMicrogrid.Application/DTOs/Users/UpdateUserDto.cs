using System.ComponentModel.DataAnnotations;
using SmartSolarMicrogrid.Domain.Enums;

namespace SmartSolarMicrogrid.Application.DTOs.Users;

public class UpdateUserDto
{
    [EmailAddress]
    public string? Email { get; set; }
    public UserRole? Role { get; set; }
    public UserStatus? Status { get; set; }
    public string? Password { get; set; }
}
