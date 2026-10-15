using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.EnergySlot;
using SmartSolarMicrogrid.Application.Interfaces;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/energy-slots")]
public class EnergySlotsController(IEnergySlotService service):ControllerBase {
    [Authorize(Roles="Backoffice,GridOperator"),HttpPost] public async Task<IActionResult> Create(CreateEnergySlotDto d)=>Ok(await service.CreateAsync(d));
    [Authorize,HttpGet] public async Task<IActionResult> Get([FromQuery]string? nodeId=null,[FromQuery]bool availableOnly=false)=>Ok(await service.GetAsync(nodeId,availableOnly));
    [Authorize(Roles="Backoffice,GridOperator"),HttpPut("{id}")] public async Task<IActionResult> Update(string id,UpdateEnergySlotDto d)=>Ok(await service.UpdateAsync(id,d));
    [Authorize(Roles="GridOperator,Backoffice"),HttpPut("{id}/availability")] public async Task<IActionResult> Availability(string id,[FromQuery]double availableCapacityKwh)=>Ok(await service.UpdateAvailabilityAsync(id,availableCapacityKwh));
}
