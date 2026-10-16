// ============================================================================
// SmartSolar Microgrid System
// File: IUserService.cs
// Purpose: Service contract defining operations and abstractions for User.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Users;

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IUserService
{
    Task<UserResponseDto> CreateUserAsync(CreateUserDto request);
    Task<IEnumerable<UserResponseDto>> GetInternalUsersAsync();
    Task<UserResponseDto> UpdateUserAsync(string id, UpdateUserDto request);
    Task DeleteUserAsync(string id);
    Task ChangePasswordAsync(string userId, ChangePasswordDto request);
}
