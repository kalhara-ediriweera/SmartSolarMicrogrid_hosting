using System.ComponentModel.DataAnnotations;
using SmartSolarMicrogrid.Domain.Enums;

namespace SmartSolarMicrogrid.Application.DTOs.Users;

public class UpdateUserDto
{
    public UserRole? Role { get; set; }
    public UserStatus? Status { get; set; }
    public string? Password { get; set; }
}
