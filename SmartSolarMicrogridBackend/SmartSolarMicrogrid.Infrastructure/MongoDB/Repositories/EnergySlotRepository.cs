using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class EnergySlotRepository : MongoRepository<EnergySlot> {
    public EnergySlotRepository(MongoDbContext db) : base(db.EnergySlots) { }
}
