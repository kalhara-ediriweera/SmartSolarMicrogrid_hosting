// ============================================================================
// SmartSolar Microgrid System
// File: EnergySlotRepository.cs
// Purpose: Data access repository handling MongoDB persistence and queries for EnergySlot.
// ============================================================================

using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

public class EnergySlotRepository : MongoRepository<EnergySlot>
{
    public EnergySlotRepository(MongoDbContext db) : base(db.EnergySlots) { }
}
