// ============================================================================
// SmartSolar Microgrid System
// File: ChangePasswordDto.cs
// Purpose: Data transfer object representing ChangePassword payload.
// ============================================================================

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Application.DTOs.Users;

public class ChangePasswordDto
{
    public string? CurrentPassword { get; set; }

    [Required]
    public string NewPassword { get; set; } = string.Empty;
}
