// ============================================================================
// SmartSolar Microgrid System
// File: UserRepository.cs
// Purpose: Data access repository handling MongoDB persistence and queries for User.
// ============================================================================

using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

public class UserRepository : MongoRepository<User>
{
    public UserRepository(MongoDbContext db) : base(db.Users) { }
}
