// ============================================================================
// SmartSolar Microgrid System
// File: CreateUserDto.cs
// Purpose: Data transfer object representing CreateUser payload.
// ============================================================================

using System.ComponentModel.DataAnnotations;
using SmartSolarMicrogrid.Domain.Enums;

namespace SmartSolarMicrogrid.Application.DTOs.Users;

public class CreateUserDto
{
    [Required]
    public string Username { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    public string Password { get; set; } = string.Empty;

    [Required]
    public UserRole Role { get; set; }

    public UserStatus Status { get; set; } = UserStatus.Active;
}
