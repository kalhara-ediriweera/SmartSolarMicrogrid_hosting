using SmartSolarMicrogrid.Application.DTOs.Users;

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IUserService
{
    Task<UserResponseDto> CreateUserAsync(CreateUserDto request);
    Task<IEnumerable<UserResponseDto>> GetInternalUsersAsync();
    Task<UserResponseDto> UpdateUserAsync(string id, UpdateUserDto request);
    Task DeleteUserAsync(string id);
}
