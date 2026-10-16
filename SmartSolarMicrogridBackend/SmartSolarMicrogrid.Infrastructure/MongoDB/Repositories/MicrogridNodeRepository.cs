// ============================================================================
// SmartSolar Microgrid System
// File: MicrogridNodeRepository.cs
// Purpose: Data access repository handling MongoDB persistence and queries for MicrogridNode.
// ============================================================================

using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

public class MicrogridNodeRepository : MongoRepository<MicrogridNode>
{
    public MicrogridNodeRepository(MongoDbContext db) : base(db.MicrogridNodes) { }
}
