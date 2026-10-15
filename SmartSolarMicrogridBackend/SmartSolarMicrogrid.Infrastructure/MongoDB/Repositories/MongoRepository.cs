using MongoDB.Driver;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public abstract class MongoRepository<T> where T: class {
    protected readonly IMongoCollection<T> Collection;
    protected MongoRepository(IMongoCollection<T> collection) => Collection = collection;
    public Task<List<T>> GetAllAsync() => Collection.Find(FilterDefinition<T>.Empty).ToListAsync();
    public Task<T?> GetByIdAsync(string id) => Collection.Find(Builders<T>.Filter.Eq("_id", id)).FirstOrDefaultAsync();
    public Task InsertAsync(T item) => Collection.InsertOneAsync(item);
    public Task ReplaceAsync(string id,T item) => Collection.ReplaceOneAsync(Builders<T>.Filter.Eq("_id", id),item);
    public Task DeleteAsync(string id) => Collection.DeleteOneAsync(Builders<T>.Filter.Eq("_id", id));
}
