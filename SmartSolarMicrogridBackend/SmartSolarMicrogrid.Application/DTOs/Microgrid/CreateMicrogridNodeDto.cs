namespace SmartSolarMicrogrid.Application.DTOs.Microgrid;
public record CreateMicrogridNodeDto(string NodeCode, string NodeName, double Latitude, double Longitude, double CapacityKw, int BatterySlotAvailability, string ScheduleStart, string ScheduleEnd);
