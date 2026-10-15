namespace SmartSolarMicrogrid.Application.DTOs.EnergySlot;
public record CreateEnergySlotDto(string NodeId, DateTime SlotDate, string StartTime, string EndTime, double EnergyAmountKwh);
