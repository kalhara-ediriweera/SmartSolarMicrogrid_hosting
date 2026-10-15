using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class SlotUnavailabilityRequestRepository : MongoRepository<SlotUnavailabilityRequest> {
    public SlotUnavailabilityRequestRepository(MongoDbContext db) : base(db.SlotUnavailabilityRequests) { }
}
