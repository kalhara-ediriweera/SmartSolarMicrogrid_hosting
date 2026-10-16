// ============================================================================
// SmartSolar Microgrid System
// File: IDashboardService.cs
// Purpose: Contract for aggregating operational dashboard metrics across prosumer,
//          grid operator, and backoffice administrative roles.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Dashboard;

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IDashboardService
{
    Task<ProsumerDashboardDto> ProsumerAsync(string nic);
    Task<OperatorDashboardDto> OperatorAsync();
    Task<BackofficeDashboardDto> BackofficeAsync();
}