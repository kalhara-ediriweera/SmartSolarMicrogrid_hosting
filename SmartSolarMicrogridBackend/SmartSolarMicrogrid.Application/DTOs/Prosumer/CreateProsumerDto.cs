namespace SmartSolarMicrogrid.Application.DTOs.Prosumer;
public record CreateProsumerDto(string NIC, string FullName, string Email, string Phone, string Address, string Username, string Password, string NicFrontImageBase64 = "", string NicBackImageBase64 = "");
