using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class QRTransactionRepository : MongoRepository<QRTransaction> {
    public QRTransactionRepository(MongoDbContext db) : base(db.QRTransactions) { }
}
