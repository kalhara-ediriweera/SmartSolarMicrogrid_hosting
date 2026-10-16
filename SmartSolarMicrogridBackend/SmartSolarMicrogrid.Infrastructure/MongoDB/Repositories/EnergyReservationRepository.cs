// ============================================================================
// SmartSolar Microgrid System
// File: EnergyReservationRepository.cs
// Purpose: Data access repository handling MongoDB persistence and queries for EnergyReservation.
// ============================================================================

using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

public class EnergyReservationRepository : MongoRepository<EnergyReservation>
{
    public EnergyReservationRepository(MongoDbContext db) : base(db.Reservations) { }
}
