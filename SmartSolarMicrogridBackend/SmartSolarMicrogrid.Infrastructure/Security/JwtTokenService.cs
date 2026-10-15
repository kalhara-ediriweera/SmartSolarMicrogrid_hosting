using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.Security;
public class JwtTokenService {
    private readonly IConfiguration _config;
    public JwtTokenService(IConfiguration config) => _config = config;
    public string Create(User user) {
        var key = _config["Jwt:Key"] ?? throw new InvalidOperationException("JWT key missing.");
        var claims = new[] {
            new Claim(JwtRegisteredClaimNames.Sub,user.Id),
            new Claim(ClaimTypes.Name,user.Username),
            new Claim(ClaimTypes.Role,user.Role.ToString())
        };
        var credentials = new SigningCredentials(new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key)),SecurityAlgorithms.HmacSha256);
        var token = new JwtSecurityToken(
            issuer:_config["Jwt:Issuer"], audience:_config["Jwt:Audience"],
            claims:claims, expires:DateTime.UtcNow.AddHours(8), signingCredentials:credentials);
        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
