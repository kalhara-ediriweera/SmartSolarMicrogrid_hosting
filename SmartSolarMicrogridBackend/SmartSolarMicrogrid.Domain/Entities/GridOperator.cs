using SmartSolarMicrogrid.Domain.Enums;
namespace SmartSolarMicrogrid.Domain.Entities;
public class GridOperator {
    public string Id { get; set; } = "";
    public string UserId { get; set; } = "";
    public string EmployeeCode { get; set; } = "";
    public string FullName { get; set; } = "";
    public string StationId { get; set; } = "";
    public UserStatus Status { get; set; } = UserStatus.Active;
}
