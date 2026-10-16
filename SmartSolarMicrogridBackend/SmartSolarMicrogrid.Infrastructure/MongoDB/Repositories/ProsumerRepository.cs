// ============================================================================
// SmartSolar Microgrid System
// File: ProsumerRepository.cs
// Purpose: Data access repository handling MongoDB persistence and queries for Prosumer.
// ============================================================================

using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

public class ProsumerRepository : MongoRepository<Prosumer>
{
    public ProsumerRepository(MongoDbContext db) : base(db.Prosumers) { }
}
