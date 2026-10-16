// ============================================================================
// SmartSolar Microgrid System
// File: IQRTransactionService.cs
// Purpose: Contract for generating, validating, and completing secure QR code
//          transactions for energy exchange at microgrid nodes.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.QR;
using SmartSolarMicrogrid.Domain.Entities;

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IQRTransactionService
{
    Task<QRTransaction> GenerateAsync(string reservationId);
    Task<object> VerifyAsync(VerifyQRDto dto);
    Task<object> CompleteAsync(VerifyQRDto dto);
}