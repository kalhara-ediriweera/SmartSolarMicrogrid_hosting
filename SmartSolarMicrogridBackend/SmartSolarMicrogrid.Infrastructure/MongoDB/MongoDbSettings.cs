namespace SmartSolarMicrogrid.Infrastructure.MongoDB;
public class MongoDbSettings {
    public string ConnectionString { get; set; } = "mongodb://localhost:27017";
    public string DatabaseName { get; set; } = "SmartSolarMicrogridDB";
}
