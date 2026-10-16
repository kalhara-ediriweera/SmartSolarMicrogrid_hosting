// ============================================================================
// SmartSolar Microgrid System
// File: SlotUnavailabilityRequestRepository.cs
// Purpose: Data access repository handling MongoDB persistence and queries for SlotUnavailabilityRequest.
// ============================================================================

using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

public class SlotUnavailabilityRequestRepository : MongoRepository<SlotUnavailabilityRequest>
{
    public SlotUnavailabilityRequestRepository(MongoDbContext db) : base(db.SlotUnavailabilityRequests) { }
}
