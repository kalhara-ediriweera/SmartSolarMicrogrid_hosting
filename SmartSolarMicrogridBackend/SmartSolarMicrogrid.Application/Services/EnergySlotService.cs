using SmartSolarMicrogrid.Application.DTOs.EnergySlot;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using System.Globalization;
namespace SmartSolarMicrogrid.Application.Services;
public class EnergySlotService : IEnergySlotService {
    private readonly EnergySlotRepository _slots; private readonly MicrogridNodeRepository _nodes;
    public EnergySlotService(EnergySlotRepository slots,MicrogridNodeRepository nodes){_slots=slots;_nodes=nodes;}
    private static TimeOnly ParseSlotTime(string value,string fieldName){
        if(!TimeOnly.TryParseExact(value,"HH:mm",CultureInfo.InvariantCulture,DateTimeStyles.None,out var time))throw new ArgumentException($"{fieldName} must use HH:mm format.");
        return time;
    }
    private static double ValidateEnergyCapacity(double energyAmountKwh,MicrogridNode node,string startTime,string endTime){
        var start=ParseSlotTime(startTime,"Slot start time");
        var end=ParseSlotTime(endTime,"Slot end time");
        var durationHours=(end-start).TotalHours;
        if(durationHours<=0)throw new ArgumentException("Slot end time must be after its start time.");
        var maximumEnergyKwh=node.CapacityKw*durationHours;
        if(energyAmountKwh>maximumEnergyKwh+1e-9)throw new ArgumentException($"Total energy cannot exceed {maximumEnergyKwh:0.##} kWh for this node and slot duration.");
        return maximumEnergyKwh;
    }
    private static DateTime GetSlotStartUtc(DateTime slotDate,string startTime){
        var time=ParseSlotTime(startTime,"Slot start time");
        return DateTime.SpecifyKind(slotDate.Date.Add(time.ToTimeSpan()),DateTimeKind.Utc);
    }
    public async Task<EnergySlot> CreateAsync(CreateEnergySlotDto d){
        var node=await _nodes.GetByIdAsync(d.NodeId)??throw new KeyNotFoundException("Node not found.");
        if(node.Status!=NodeStatus.Active)throw new InvalidOperationException("Cannot create slot for inactive node.");
        Validation.Positive(d.EnergyAmountKwh,"EnergyAmountKwh");
        ValidateEnergyCapacity(d.EnergyAmountKwh,node,d.StartTime,d.EndTime);
        if(GetSlotStartUtc(d.SlotDate,d.StartTime) <= DateTime.UtcNow)throw new ArgumentException("Energy slot must start in the future.");

        var allSlots = await _slots.GetAllAsync();
        var duplicate = allSlots.FirstOrDefault(x =>
            x.NodeId == d.NodeId &&
            x.SlotDate.Date == d.SlotDate.Date &&
            x.StartTime.Trim() == d.StartTime.Trim() &&
            x.EndTime.Trim() == d.EndTime.Trim());

        if (duplicate != null) {
            throw new InvalidOperationException($"An energy slot already exists for station '{node.NodeName}' on {d.SlotDate:yyyy-MM-dd} ({d.StartTime} - {d.EndTime}). Please update the existing slot's energy capacity instead.");
        }

        var s=new EnergySlot{Id=Guid.NewGuid().ToString(),NodeId=d.NodeId,SlotDate=d.SlotDate.Date,StartTime=d.StartTime,EndTime=d.EndTime,EnergyAmountKwh=d.EnergyAmountKwh,AvailableCapacityKwh=d.EnergyAmountKwh};
        await _slots.InsertAsync(s);return s;
    }
    public async Task<IReadOnlyList<EnergySlot>> GetAsync(string? nodeId=null,bool availableOnly=false){
        var x=await _slots.GetAllAsync();if(!string.IsNullOrWhiteSpace(nodeId))x=x.Where(s=>s.NodeId==nodeId).ToList();if(availableOnly)x=x.Where(s=>s.Status==SlotStatus.Available&&s.AvailableCapacityKwh>0).ToList();return x.OrderBy(s=>s.SlotDate).ThenBy(s=>s.StartTime).ToList();
    }
    public async Task<EnergySlot> UpdateAsync(string id,UpdateEnergySlotDto d){
        var s=await _slots.GetByIdAsync(id)??throw new KeyNotFoundException("Slot not found.");
        if(GetSlotStartUtc(s.SlotDate,s.StartTime) <= DateTime.UtcNow)throw new InvalidOperationException("Past or started energy slots cannot be updated.");
        if(GetSlotStartUtc(d.SlotDate,d.StartTime) <= DateTime.UtcNow)throw new ArgumentException("Updated energy slot must start in the future.");
        var node=await _nodes.GetByIdAsync(s.NodeId)??throw new KeyNotFoundException("Node not found.");
        ValidateEnergyCapacity(d.EnergyAmountKwh,node,d.StartTime,d.EndTime);
        if(Enum.TryParse<SlotStatus>(d.Status,true,out var st)){
            if(node.Status != NodeStatus.Active && st == SlotStatus.Available)
                throw new InvalidOperationException($"Cannot set slot to Available because microgrid node '{node.NodeName}' is deactivated ({node.AdminNote ?? "Inactive"}).");
            s.Status=st;
        } else throw new ArgumentException("Invalid slot status.");

        var allSlots = await _slots.GetAllAsync();
        var duplicate = allSlots.FirstOrDefault(x =>
            x.Id != id &&
            x.NodeId == s.NodeId &&
            x.SlotDate.Date == d.SlotDate.Date &&
            x.StartTime.Trim() == d.StartTime.Trim() &&
            x.EndTime.Trim() == d.EndTime.Trim());

        if (duplicate != null) {
            throw new InvalidOperationException($"Another energy slot already exists for this station on {d.SlotDate:yyyy-MM-dd} ({d.StartTime} - {d.EndTime}).");
        }

        Validation.Positive(d.EnergyAmountKwh, "EnergyAmountKwh");

        // Automatically preserve reserved energy and update available capacity
        double reservedCapacity = Math.Max(0, s.EnergyAmountKwh - s.AvailableCapacityKwh);
        if (d.EnergyAmountKwh < reservedCapacity) {
            throw new InvalidOperationException($"Cannot reduce total energy below already reserved capacity ({reservedCapacity:0.##} kWh).");
        }

        double newAvailable = Math.Max(0, d.EnergyAmountKwh - reservedCapacity);

        s.SlotDate=d.SlotDate.Date;
        s.StartTime=d.StartTime;
        s.EndTime=d.EndTime;
        s.EnergyAmountKwh=d.EnergyAmountKwh;
        s.AvailableCapacityKwh=newAvailable;

        if (newAvailable <= 0 && s.Status == SlotStatus.Available) {
            s.Status = SlotStatus.Reserved;
        } else if (newAvailable > 0 && s.Status == SlotStatus.Reserved) {
            s.Status = SlotStatus.Available;
        }

        s.UpdatedAt=DateTime.UtcNow;
        await _slots.ReplaceAsync(id,s);
        return s;
    }
    public async Task<EnergySlot> UpdateAvailabilityAsync(string id,double available){
        var s=await _slots.GetByIdAsync(id)??throw new KeyNotFoundException("Slot not found.");
        if(GetSlotStartUtc(s.SlotDate,s.StartTime) <= DateTime.UtcNow)throw new InvalidOperationException("Past or started energy slots cannot be updated.");
        var node=await _nodes.GetByIdAsync(s.NodeId);
        if(node != null && node.Status != NodeStatus.Active && available > 0)
            throw new InvalidOperationException($"Cannot make slot available because microgrid node '{node.NodeName}' is deactivated ({node.AdminNote ?? "Inactive"}).");
        if(available<0 || available>s.EnergyAmountKwh)throw new ArgumentException("Invalid availability.");
        s.AvailableCapacityKwh=available;s.Status=available>0?SlotStatus.Available:SlotStatus.Unavailable;s.UpdatedAt=DateTime.UtcNow;await _slots.ReplaceAsync(id,s);return s;
    }
}
