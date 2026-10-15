using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class EnergyReservationRepository : MongoRepository<EnergyReservation> {
    public EnergyReservationRepository(MongoDbContext db) : base(db.Reservations) { }
}
