// ============================================================================
// SmartSolar Microgrid System
// File: IOcrService.cs
// Purpose: Service contract defining operations and abstractions for Ocr.
// ============================================================================

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IOcrService
{
    Task<string?> ExtractNicFromImageAsync(string base64Image, string expectedNic);
}
