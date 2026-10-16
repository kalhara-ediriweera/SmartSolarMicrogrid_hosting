// ============================================================================
// SmartSolar Microgrid System
// File: MongoRepository.cs
// Purpose: Generic MongoDB repository base providing standard CRUD operations
//          for collection entities.
// ============================================================================

using MongoDB.Driver;

namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

public abstract class MongoRepository<T> where T : class
{
    protected readonly IMongoCollection<T> Collection;

    protected MongoRepository(IMongoCollection<T> collection)
    {
        // Initialize required dependencies and services
        Collection = collection;
    }

    public virtual async Task<List<T>> GetAllAsync()
    {
        // Query and return all documents in the collection
        return await Collection.Find(FilterDefinition<T>.Empty).ToListAsync();
    }

    public virtual async Task<T?> GetByIdAsync(string id)
    {
        // Fetch a single document by its primary key identifier
        return await Collection.Find(Builders<T>.Filter.Eq("_id", id)).FirstOrDefaultAsync();
    }

    public virtual async Task InsertAsync(T item)
    {
        // Persist a new entity document into the MongoDB collection
        await Collection.InsertOneAsync(item);
    }

    public virtual async Task ReplaceAsync(string id, T item)
    {
        // Replace an existing entity document matching the given identifier
        await Collection.ReplaceOneAsync(Builders<T>.Filter.Eq("_id", id), item);
    }

    public virtual async Task DeleteAsync(string id)
    {
        // Remove an entity document permanently from the collection
        await Collection.DeleteOneAsync(Builders<T>.Filter.Eq("_id", id));
    }
}
