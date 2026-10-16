// ============================================================================
// SmartSolar Microgrid System
// File: GridOperatorRepository.cs
// Purpose: Data access repository handling MongoDB persistence and queries for GridOperator.
// ============================================================================

using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

public class GridOperatorRepository : MongoRepository<GridOperator>
{
    public GridOperatorRepository(MongoDbContext db) : base(db.GridOperators) { }
}
