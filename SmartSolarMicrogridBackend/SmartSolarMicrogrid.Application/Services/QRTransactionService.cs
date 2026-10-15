using System.Security.Cryptography;
using System.Text;
using SmartSolarMicrogrid.Application.DTOs.QR;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
namespace SmartSolarMicrogrid.Application.Services;
public class QRTransactionService : IQRTransactionService {
    private readonly QRTransactionRepository _qrs; private readonly EnergyReservationRepository _res;
    public QRTransactionService(QRTransactionRepository qrs,EnergyReservationRepository res){_qrs=qrs;_res=res;}
    public async Task<QRTransaction> GenerateAsync(string reservationId){
        var r=await _res.GetByIdAsync(reservationId)??throw new KeyNotFoundException("Reservation not found.");
        if(r.Status!=ReservationStatus.Approved)throw new InvalidOperationException("QR can only be generated after reservation approval.");
        var existing=(await _qrs.GetAllAsync()).FirstOrDefault(x=>x.ReservationId==reservationId&&x.Status==QRStatus.Generated);
        if(existing is not null)return existing;
        var raw=$"{reservationId}:{Guid.NewGuid()}:{DateTime.UtcNow.Ticks}";
        var token=Convert.ToBase64String(SHA256.HashData(Encoding.UTF8.GetBytes(raw))).Replace("+","-").Replace("/","_").TrimEnd('=');
        var q=new QRTransaction{Id=Guid.NewGuid().ToString(),ReservationId=reservationId,TransactionCode="TX-"+Random.Shared.Next(100000,999999),QRToken=token};
        await _qrs.InsertAsync(q);return q;
    }
    public async Task<object> VerifyAsync(VerifyQRDto dto){
        var q=(await _qrs.GetAllAsync()).FirstOrDefault(x=>x.QRToken==dto.QRToken);
        if(q is null)throw new UnauthorizedAccessException("Invalid QR token.");
        if(q.Status==QRStatus.Used)throw new InvalidOperationException("Transaction already used.");
        var r=await _res.GetByIdAsync(q.ReservationId)??throw new KeyNotFoundException("Reservation not found.");
        if(r.Status!=ReservationStatus.Approved)throw new InvalidOperationException("Reservation is not approved.");
        q.Status=QRStatus.Verified;q.ScannedAt=DateTime.UtcNow;q.VerifiedAt=DateTime.UtcNow;await _qrs.ReplaceAsync(q.Id,q);
        return new {valid=true,q.TransactionCode,reservation=r};
    }
    public async Task<object> CompleteAsync(VerifyQRDto dto){
        var q=(await _qrs.GetAllAsync()).FirstOrDefault(x=>x.QRToken==dto.QRToken);
        if(q is null)throw new UnauthorizedAccessException("Invalid QR token.");
        if(q.Status!=QRStatus.Verified)throw new InvalidOperationException("QR must be verified before completion.");
        var r=await _res.GetByIdAsync(q.ReservationId)??throw new KeyNotFoundException("Reservation not found.");
        if(r.Status!=ReservationStatus.Approved)throw new InvalidOperationException("Reservation is not approved.");
        r.Status=ReservationStatus.Completed;r.CompletedAt=DateTime.UtcNow;q.Status=QRStatus.Used;q.CompletedAt=DateTime.UtcNow;
        await _res.ReplaceAsync(r.Id,r);await _qrs.ReplaceAsync(q.Id,q);
        return new {completed=true,transactionCode=q.TransactionCode,reservation=r};
    }
}
