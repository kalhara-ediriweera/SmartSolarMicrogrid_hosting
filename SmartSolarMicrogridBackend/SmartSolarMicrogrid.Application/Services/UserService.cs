using SmartSolarMicrogrid.Application.DTOs.Users;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;
using SmartSolarMicrogrid.Infrastructure.Interfaces;

namespace SmartSolarMicrogrid.Application.Services;

public class UserService : IUserService
{
    private readonly UserRepository _users;
    private readonly PasswordHasher _hasher;
    private readonly IEmailService _emailService;

    public UserService(UserRepository users, PasswordHasher hasher, IEmailService emailService)
    {
        _users = users;
        _hasher = hasher;
        _emailService = emailService;
    }

    public async Task<UserResponseDto> CreateUserAsync(CreateUserDto request)
    {
        if (request.Role == UserRole.Prosumer)
            throw new ArgumentException("Prosumer accounts cannot be created via the internal user management endpoint.");

        if (string.IsNullOrWhiteSpace(request.Username))
            throw new ArgumentException("Username is required.");

        UserValidator.ValidateEmail(request.Email);
        UserValidator.ValidatePassword(request.Password);

        var existingUsers = await _users.GetAllAsync();
        if (existingUsers.Any(u => u.Username.Equals(request.Username.Trim(), StringComparison.OrdinalIgnoreCase)))
            throw new InvalidOperationException("Username already exists.");

        var cleanEmail = request.Email.Trim().ToLowerInvariant();
        if (existingUsers.Any(u => !string.IsNullOrEmpty(u.Email) && u.Email.Equals(cleanEmail, StringComparison.OrdinalIgnoreCase)))
            throw new InvalidOperationException("Email address is already registered to another user.");

        var user = new User
        {
            Id = Guid.NewGuid().ToString(),
            Username = request.Username.Trim(),
            Email = cleanEmail,
            PasswordHash = _hasher.Hash(request.Password),
            Role = request.Role,
            Status = request.Status,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        await _users.InsertAsync(user);

        // Send email displaying username and password created by admin
        await _emailService.SendUserWelcomeEmailAsync(user.Email, user.Username, request.Password, user.Role.ToString());

        return new UserResponseDto
        {
            Id = user.Id,
            Username = user.Username,
            Email = user.Email,
            Role = user.Role,
            Status = user.Status,
            CreatedAt = user.CreatedAt
        };
    }

    public async Task<IEnumerable<UserResponseDto>> GetInternalUsersAsync()
    {
        var users = await _users.GetAllAsync();
        
        return users
            .Where(u => u.Role != UserRole.Prosumer)
            .Select(u => new UserResponseDto
            {
                Id = u.Id,
                Username = u.Username,
                Email = u.Email ?? string.Empty,
                Role = u.Role,
                Status = u.Status,
                CreatedAt = u.CreatedAt
            })
            .OrderByDescending(u => u.CreatedAt);
    }

    public async Task<UserResponseDto> UpdateUserAsync(string id, UpdateUserDto request)
    {
        var user = await _users.GetByIdAsync(id);
        if (user == null)
            throw new KeyNotFoundException("User not found.");

        if (!string.IsNullOrWhiteSpace(request.Email))
        {
            UserValidator.ValidateEmail(request.Email);
            var cleanEmail = request.Email.Trim().ToLowerInvariant();
            var existingUsers = await _users.GetAllAsync();
            if (existingUsers.Any(u => u.Id != id && !string.IsNullOrEmpty(u.Email) && u.Email.Equals(cleanEmail, StringComparison.OrdinalIgnoreCase)))
                throw new InvalidOperationException("Email address is already registered to another user.");
            user.Email = cleanEmail;
        }

        if (request.Role.HasValue)
            user.Role = request.Role.Value;
            
        if (request.Status.HasValue)
            user.Status = request.Status.Value;

        if (!string.IsNullOrEmpty(request.Password))
        {
            UserValidator.ValidatePassword(request.Password);
            user.PasswordHash = _hasher.Hash(request.Password);
        }

        user.UpdatedAt = DateTime.UtcNow;

        await _users.ReplaceAsync(id, user);

        return new UserResponseDto
        {
            Id = user.Id,
            Username = user.Username,
            Email = user.Email,
            Role = user.Role,
            Status = user.Status,
            CreatedAt = user.CreatedAt
        };
    }

    public async Task ChangePasswordAsync(string userId, ChangePasswordDto request)
    {
        var user = await _users.GetByIdAsync(userId);
        if (user == null)
            throw new KeyNotFoundException("User not found.");

        if (!string.IsNullOrEmpty(request.CurrentPassword))
        {
            if (!_hasher.Verify(request.CurrentPassword, user.PasswordHash))
                throw new UnauthorizedAccessException("Current password is incorrect.");
        }

        UserValidator.ValidatePassword(request.NewPassword);

        user.PasswordHash = _hasher.Hash(request.NewPassword);
        user.UpdatedAt = DateTime.UtcNow;

        await _users.ReplaceAsync(userId, user);
    }

    public async Task DeleteUserAsync(string id)
    {
        var user = await _users.GetByIdAsync(id);
        if (user == null)
            throw new KeyNotFoundException("User not found.");
            
        await _users.DeleteAsync(id);
    }
}
