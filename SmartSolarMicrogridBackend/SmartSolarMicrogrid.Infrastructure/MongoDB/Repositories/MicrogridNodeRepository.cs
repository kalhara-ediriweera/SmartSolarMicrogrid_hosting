using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class MicrogridNodeRepository : MongoRepository<MicrogridNode> {
    public MicrogridNodeRepository(MongoDbContext db) : base(db.MicrogridNodes) { }
}
