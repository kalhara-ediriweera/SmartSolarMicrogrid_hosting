using SmartSolarMicrogrid.Application.DTOs.EnergySlot;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
namespace SmartSolarMicrogrid.Application.Services;
public class EnergySlotService : IEnergySlotService {
    private readonly EnergySlotRepository _slots; private readonly MicrogridNodeRepository _nodes;
    public EnergySlotService(EnergySlotRepository slots,MicrogridNodeRepository nodes){_slots=slots;_nodes=nodes;}
    public async Task<EnergySlot> CreateAsync(CreateEnergySlotDto d){
        var node=await _nodes.GetByIdAsync(d.NodeId)??throw new KeyNotFoundException("Node not found.");
        if(node.Status!=NodeStatus.Active)throw new InvalidOperationException("Cannot create slot for inactive node.");
        Validation.Positive(d.EnergyAmountKwh,"EnergyAmountKwh");
        if(d.SlotDate.Date < DateTime.UtcNow.Date || d.SlotDate.Date > DateTime.UtcNow.Date.AddDays(7))throw new ArgumentException("Slot must be within the allowed 7-day period.");
        var s=new EnergySlot{Id=Guid.NewGuid().ToString(),NodeId=d.NodeId,SlotDate=d.SlotDate.Date,StartTime=d.StartTime,EndTime=d.EndTime,EnergyAmountKwh=d.EnergyAmountKwh,AvailableCapacityKwh=d.EnergyAmountKwh};
        await _slots.InsertAsync(s);return s;
    }
    public async Task<IReadOnlyList<EnergySlot>> GetAsync(string? nodeId=null,bool availableOnly=false){
        var x=await _slots.GetAllAsync();if(nodeId is not null)x=x.Where(s=>s.NodeId==nodeId).ToList();if(availableOnly)x=x.Where(s=>s.Status==SlotStatus.Available&&s.AvailableCapacityKwh>0).ToList();return x.OrderBy(s=>s.SlotDate).ThenBy(s=>s.StartTime).ToList();
    }
    public async Task<EnergySlot> UpdateAsync(string id,UpdateEnergySlotDto d){
        var s=await _slots.GetByIdAsync(id)??throw new KeyNotFoundException("Slot not found.");
        var node=await _nodes.GetByIdAsync(s.NodeId);
        if(Enum.TryParse<SlotStatus>(d.Status,true,out var st)){
            if(node != null && node.Status != NodeStatus.Active && st == SlotStatus.Available)
                throw new InvalidOperationException($"Cannot set slot to Available because microgrid node '{node.NodeName}' is deactivated ({node.AdminNote ?? "Inactive"}).");
            s.Status=st;
        } else throw new ArgumentException("Invalid slot status.");
        s.SlotDate=d.SlotDate.Date;s.StartTime=d.StartTime;s.EndTime=d.EndTime;s.EnergyAmountKwh=d.EnergyAmountKwh;s.AvailableCapacityKwh=d.AvailableCapacityKwh;
        s.UpdatedAt=DateTime.UtcNow;await _slots.ReplaceAsync(id,s);return s;
    }
    public async Task<EnergySlot> UpdateAvailabilityAsync(string id,double available){
        var s=await _slots.GetByIdAsync(id)??throw new KeyNotFoundException("Slot not found.");
        var node=await _nodes.GetByIdAsync(s.NodeId);
        if(node != null && node.Status != NodeStatus.Active && available > 0)
            throw new InvalidOperationException($"Cannot make slot available because microgrid node '{node.NodeName}' is deactivated ({node.AdminNote ?? "Inactive"}).");
        if(available<0 || available>s.EnergyAmountKwh)throw new ArgumentException("Invalid availability.");
        s.AvailableCapacityKwh=available;s.Status=available>0?SlotStatus.Available:SlotStatus.Unavailable;s.UpdatedAt=DateTime.UtcNow;await _slots.ReplaceAsync(id,s);return s;
    }
}
