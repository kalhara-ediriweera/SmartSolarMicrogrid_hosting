// ============================================================================
// SmartSolar Microgrid System
// File: MongoDbContext.cs
// Purpose: Core component for MongoDbContext within the SmartSolar microgrid platform.
// ============================================================================

using Microsoft.Extensions.Options;
using MongoDB.Driver;
using MongoDB.Bson.Serialization.Conventions;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB;

public class MongoDbContext
{
    private readonly IMongoDatabase _db;
    public MongoDbContext(IOptions<MongoDbSettings> options)
    {
        // Initialize required dependencies and services
        var pack = new ConventionPack { new IgnoreExtraElementsConvention(true) };
        ConventionRegistry.Register("IgnoreExtraElements", pack, t => true);
        var client = new MongoClient(options.Value.ConnectionString);
        _db = client.GetDatabase(options.Value.DatabaseName);
    }
    public IMongoCollection<User> Users => _db.GetCollection<User>("Users");
    public IMongoCollection<Prosumer> Prosumers => _db.GetCollection<Prosumer>("Prosumers");
    public IMongoCollection<GridOperator> GridOperators => _db.GetCollection<GridOperator>("GridOperators");
    public IMongoCollection<MicrogridNode> MicrogridNodes => _db.GetCollection<MicrogridNode>("SolarStationInfo");
    public IMongoCollection<EnergySlot> EnergySlots => _db.GetCollection<EnergySlot>("EnergyBookingSlots");
    public IMongoCollection<EnergyReservation> Reservations => _db.GetCollection<EnergyReservation>("EnergyReservations");
    public IMongoCollection<QRTransaction> QRTransactions => _db.GetCollection<QRTransaction>("QRTransactions");
    public IMongoCollection<SlotUnavailabilityRequest> SlotUnavailabilityRequests => _db.GetCollection<SlotUnavailabilityRequest>("SlotUnavailabilityRequests");
}
