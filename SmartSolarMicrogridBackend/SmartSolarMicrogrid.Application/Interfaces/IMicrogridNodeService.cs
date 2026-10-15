using SmartSolarMicrogrid.Application.DTOs.Microgrid;
using SmartSolarMicrogrid.Domain.Entities;

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IMicrogridNodeService
{
    Task<MicrogridNode> CreateAsync(CreateMicrogridNodeDto dto);
    Task<IReadOnlyList<MicrogridNode>> GetAllAsync(bool activeOnly = false);
    Task<MicrogridNode?> GetAsync(string id);
    Task<MicrogridNode> UpdateAsync(string id, UpdateMicrogridNodeDto dto);
    Task<MicrogridNode> DeactivateAsync(string id, string? reason = null);
    Task<MicrogridNode> ActivateAsync(string id);
    Task<MicrogridNode> UpdateScheduleAsync(string id, string start, string end);
}