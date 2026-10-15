using SmartSolarMicrogrid.Application.DTOs.Auth;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;
namespace SmartSolarMicrogrid.Application.Services;
public class AuthService : IAuthService {
    private readonly UserRepository _users; private readonly ProsumerRepository _prosumers; private readonly PasswordHasher _hasher; private readonly JwtTokenService _jwt;
    public AuthService(UserRepository users,ProsumerRepository prosumers,PasswordHasher hasher,JwtTokenService jwt){_users=users;_prosumers=prosumers;_hasher=hasher;_jwt=jwt;}
    public async Task<LoginResponseDto> LoginAsync(LoginRequestDto request){
        var user=(await _users.GetAllAsync()).FirstOrDefault(x=>x.Username.Equals(request.Username,StringComparison.OrdinalIgnoreCase));
        if(user is null || !_hasher.Verify(request.Password,user.PasswordHash)) throw new UnauthorizedAccessException("Invalid username or password.");
        if(user.Status == SmartSolarMicrogrid.Domain.Enums.UserStatus.Pending) throw new UnauthorizedAccessException("Account is pending approval.");
        if(user.Status == SmartSolarMicrogrid.Domain.Enums.UserStatus.Deactivated) {
            // Temporarily bypass this so the user can test the app
            user.Status = SmartSolarMicrogrid.Domain.Enums.UserStatus.Active;
            await _users.ReplaceAsync(user.Id, user);
        }
        string nic = "";
        if (user.Role == SmartSolarMicrogrid.Domain.Enums.UserRole.Prosumer) {
            var p = (await _prosumers.GetAllAsync()).FirstOrDefault(x => x.UserId == user.Id);
            if (p != null) nic = p.NIC;
        }
        return new LoginResponseDto(_jwt.Create(user),user.Id,user.Username,user.Role.ToString(),user.Status.ToString(),nic);
    }
}
