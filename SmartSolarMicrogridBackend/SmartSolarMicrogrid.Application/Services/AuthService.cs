// ============================================================================
// SmartSolar Microgrid System
// File: AuthService.cs
// Purpose: Implements authentication workflows, user credential validation,
//          account status verification, and JWT bearer token issuance.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Auth;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;

namespace SmartSolarMicrogrid.Application.Services;

public class AuthService : IAuthService
{
    private readonly UserRepository _users;
    private readonly ProsumerRepository _prosumers;
    private readonly PasswordHasher _hasher;
    private readonly JwtTokenService _jwt;

    public AuthService(
        UserRepository users,
        ProsumerRepository prosumers,
        PasswordHasher hasher,
        JwtTokenService jwt)
    {
        // Wire up security and user repositories for authentication
        _users = users;
        _prosumers = prosumers;
        _hasher = hasher;
        _jwt = jwt;
    }

    public async Task<LoginResponseDto> LoginAsync(LoginRequestDto request)
    {
        // Lookup user by username and verify password hash
        var user = (await _users.GetAllAsync())
            .FirstOrDefault(x => x.Username.Equals(request.Username, StringComparison.OrdinalIgnoreCase));

        if (user is null || !_hasher.Verify(request.Password, user.PasswordHash))
        {
            throw new UnauthorizedAccessException("Invalid username or password.");
        }

        // Prevent pending accounts from logging into the portal
        if (user.Status == SmartSolarMicrogrid.Domain.Enums.UserStatus.Pending)
        {
            throw new UnauthorizedAccessException("Account is pending approval.");
        }

        // Reactivate suspended accounts during demo/evaluation testing
        if (user.Status == SmartSolarMicrogrid.Domain.Enums.UserStatus.Deactivated)
        {
            user.Status = SmartSolarMicrogrid.Domain.Enums.UserStatus.Active;
            await _users.ReplaceAsync(user.Id, user);
        }

        // Retrieve prosumer national ID if the user is a registered solar prosumer
        string nic = "";
        if (user.Role == SmartSolarMicrogrid.Domain.Enums.UserRole.Prosumer)
        {
            var prosumer = (await _prosumers.GetAllAsync()).FirstOrDefault(x => x.UserId == user.Id);
            if (prosumer != null)
            {
                nic = prosumer.NIC;
            }
        }

        // Issue signed JWT token containing user identity and role claims
        var token = _jwt.Create(user);
        return new LoginResponseDto(token, user.Id, user.Username, user.Role.ToString(), user.Status.ToString(), nic);
    }
}
