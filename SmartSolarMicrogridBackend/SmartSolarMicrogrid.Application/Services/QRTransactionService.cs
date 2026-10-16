// ============================================================================
// SmartSolar Microgrid System
// File: QRTransactionService.cs
// Purpose: Business logic service coordinating workflows and validation for QRTransaction.
// ============================================================================

using System.Security.Cryptography;
using System.Text;
using SmartSolarMicrogrid.Application.DTOs.QR;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
namespace SmartSolarMicrogrid.Application.Services;

public class QRTransactionService : IQRTransactionService
{
    private readonly QRTransactionRepository _qrs;
    private readonly EnergyReservationRepository _res;
    private readonly ProsumerRepository _pros;
    private readonly MicrogridNodeRepository _nodes;

    public QRTransactionService(
        QRTransactionRepository qrs,
        EnergyReservationRepository res,
        ProsumerRepository pros,
        MicrogridNodeRepository nodes)
    {
        _qrs = qrs;
        _res = res;
        _pros = pros;
        _nodes = nodes;
    }

    public async Task<QRTransaction> GenerateAsync(string reservationId)
    {
        // Generate cryptographically secure token or identifier
        var r = await _res.GetByIdAsync(reservationId) ?? throw new KeyNotFoundException("Reservation not found.");
        if (r.Status != ReservationStatus.Approved) throw new InvalidOperationException("QR can only be generated after reservation approval.");
        var existing = (await _qrs.GetAllAsync()).FirstOrDefault(x => x.ReservationId == reservationId && x.Status == QRStatus.Generated);
        if (existing is not null) return existing;
        var raw = $"{reservationId}:{Guid.NewGuid()}:{DateTime.UtcNow.Ticks}";
        var token = Convert.ToBase64String(SHA256.HashData(Encoding.UTF8.GetBytes(raw))).Replace("+", "-").Replace("/", "_").TrimEnd('=');
        var q = new QRTransaction { Id = Guid.NewGuid().ToString(), ReservationId = reservationId, TransactionCode = "TX-" + Random.Shared.Next(100000, 999999), QRToken = token };
        await _qrs.InsertAsync(q); return q;
    }
    public async Task<object> VerifyAsync(VerifyQRDto dto)
    {
        // Validate business rules, format integrity, and constraints
        var q = (await _qrs.GetAllAsync()).FirstOrDefault(x => x.QRToken == dto.QRToken);
        if (q is null) throw new UnauthorizedAccessException("Invalid QR token.");
        if (q.Status == QRStatus.Used) throw new InvalidOperationException("Transaction already used.");
        var r = await _res.GetByIdAsync(q.ReservationId) ?? throw new KeyNotFoundException("Reservation not found.");
        if (r.Status != ReservationStatus.Approved) throw new InvalidOperationException("Reservation is not approved.");
        q.Status = QRStatus.Verified; q.ScannedAt = DateTime.UtcNow; q.VerifiedAt = DateTime.UtcNow; await _qrs.ReplaceAsync(q.Id, q);
        var p = await _pros.GetByIdAsync(r.ProsumerId);
        var node = await _nodes.GetByIdAsync(r.NodeId);
        return new
        {
            valid = true,
            transactionCode = q.TransactionCode,
            qrToken = q.QRToken,
            reservation = r,
            prosumerName = p?.FullName ?? r.ProsumerId,
            nodeName = node?.NodeName ?? r.NodeId
        };
    }
    public async Task<object> CompleteAsync(VerifyQRDto dto)
    {
        // Execute CompleteAsync operation logic
        var q = (await _qrs.GetAllAsync()).FirstOrDefault(x => x.QRToken == dto.QRToken);
        if (q is null) throw new UnauthorizedAccessException("Invalid QR token.");
        if (q.Status == QRStatus.Used) throw new InvalidOperationException("Transaction already used.");
        var r = await _res.GetByIdAsync(q.ReservationId) ?? throw new KeyNotFoundException("Reservation not found.");
        if (r.Status != ReservationStatus.Approved && r.Status != ReservationStatus.Completed) throw new InvalidOperationException("Reservation is not approved.");
        r.Status = ReservationStatus.Completed; r.CompletedAt = DateTime.UtcNow; q.Status = QRStatus.Used; q.CompletedAt = DateTime.UtcNow;
        await _res.ReplaceAsync(r.Id, r); await _qrs.ReplaceAsync(q.Id, q);
        var p = await _pros.GetByIdAsync(r.ProsumerId);
        return new { completed = true, transactionCode = q.TransactionCode, reservation = r, prosumerName = p?.FullName ?? r.ProsumerId };
    }
}
