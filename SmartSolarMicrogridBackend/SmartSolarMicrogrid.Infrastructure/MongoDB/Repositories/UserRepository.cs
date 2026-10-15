using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class UserRepository : MongoRepository<User> {
    public UserRepository(MongoDbContext db) : base(db.Users) { }
}
