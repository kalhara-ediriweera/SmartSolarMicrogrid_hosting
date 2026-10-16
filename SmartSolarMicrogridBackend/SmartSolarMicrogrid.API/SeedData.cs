using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;

namespace SmartSolarMicrogrid.API;

public static class SeedData
{
    public static async Task RunAsync(IServiceProvider sp)
    {
        using var scope = sp.CreateScope();

        var users = scope.ServiceProvider.GetRequiredService<UserRepository>();
        var prosumers = scope.ServiceProvider.GetRequiredService<ProsumerRepository>();
        var hasher = scope.ServiceProvider.GetRequiredService<PasswordHasher>();

        var list = await users.GetAllAsync();

        async Task<User> AddUser(string username, string password, UserRole role, string email = "", UserStatus status = UserStatus.Active)
        {
            var existing = list.FirstOrDefault(x => x.Username.Equals(username, StringComparison.OrdinalIgnoreCase));
            if (existing is not null)
            {
                if (string.IsNullOrEmpty(existing.Email) && !string.IsNullOrEmpty(email))
                {
                    existing.Email = email;
                    await users.ReplaceAsync(existing.Id, existing);
                }
                return existing;
            }

            var user = new User
            {
                Id = Guid.NewGuid().ToString(),
                Username = username,
                Email = email,
                PasswordHash = hasher.Hash(password),
                Role = role,
                Status = status
            };

            await users.InsertAsync(user);
            list.Add(user);
            return user;
        }

        await AddUser("admin", "Admin@123", UserRole.Backoffice, "admin@smartsolarmicrogrid.com");
        await AddUser("operator1", "Operator@123", UserRole.GridOperator, "operator1@smartsolarmicrogrid.com");

        var prosumerUser = await AddUser("prosumer1", "Prosumer@123", UserRole.Prosumer, "prosumer1@example.com", UserStatus.Active);

        var allProsumers = await prosumers.GetAllAsync();
        if (!allProsumers.Any(x => x.UserId == prosumerUser.Id))
        {
            await prosumers.InsertAsync(new Prosumer
            {
                Id = Guid.NewGuid().ToString(),
                NIC = "200012345678",
                FullName = "Demo Prosumer",
                Email = "prosumer1@example.com",
                Phone = "0712345678",
                Address = "Sri Lanka",
                UserId = prosumerUser.Id,
                AccountStatus = UserStatus.Active
            });
        }
    }
}
