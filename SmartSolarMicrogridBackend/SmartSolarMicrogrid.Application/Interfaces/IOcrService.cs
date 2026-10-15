namespace SmartSolarMicrogrid.Application.Interfaces;
public interface IOcrService {
    Task<string?> ExtractNicFromImageAsync(string base64Image, string expectedNic);
}
