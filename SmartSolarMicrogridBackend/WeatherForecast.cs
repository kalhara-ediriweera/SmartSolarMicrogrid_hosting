// ============================================================================
// SmartSolar Microgrid System
// File: WeatherForecast.cs
// Purpose: Core component for WeatherForecast within the SmartSolar microgrid platform.
// ============================================================================

namespace SmartSolarMicrogridBackend;

public class WeatherForecast
{
    public DateOnly Date { get; set; }

    public int TemperatureC { get; set; }

    public int TemperatureF => 32 + (int)(TemperatureC / 0.5556);

    public string? Summary { get; set; }
}
