using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class ProsumerRepository : MongoRepository<Prosumer> {
    public ProsumerRepository(MongoDbContext db) : base(db.Prosumers) { }
}
