// ============================================================================
// SmartSolar Microgrid System
// File: ProsumerService.cs
// Purpose: Implements business workflows for prosumer user registration, NIC OCR
//          processing, profile updates, and administrative lifecycle actions.
// ============================================================================

using SmartSolarMicrogrid.Application.DTOs.Prosumer;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;

namespace SmartSolarMicrogrid.Application.Services;

public class ProsumerService : IProsumerService
{
    private readonly ProsumerRepository _pros;
    private readonly UserRepository _users;
    private readonly PasswordHasher _hasher;
    private readonly IOcrService _ocr;

    public ProsumerService(
        ProsumerRepository pros,
        UserRepository users,
        PasswordHasher hasher,
        IOcrService ocr)
    {
        // Inject prosumer and identity dependencies
        _pros = pros;
        _users = users;
        _hasher = hasher;
        _ocr = ocr;
    }

    public async Task<Prosumer> RegisterAsync(CreateProsumerDto dto)
    {
        // Validate NIC, unique username, and process identity card OCR
        Validation.Required(dto.NIC, "NIC");
        Validation.NIC(dto.NIC);
        Validation.Required(dto.Username, "Username");
        Validation.Required(dto.Password, "Password");

        var allProsumers = await _pros.GetAllAsync();
        if (allProsumers.Any(x => x.NIC.Equals(dto.NIC, StringComparison.OrdinalIgnoreCase)))
        {
            throw new InvalidOperationException("NIC already exists.");
        }

        var allUsers = await _users.GetAllAsync();
        if (allUsers.Any(x => x.Username.Equals(dto.Username, StringComparison.OrdinalIgnoreCase)))
        {
            throw new InvalidOperationException("Username already exists.");
        }

        string? extractedNic = await _ocr.ExtractNicFromImageAsync(dto.NicFrontImageBase64, dto.NIC);

        var user = new User
        {
            Id = Guid.NewGuid().ToString(),
            Username = dto.Username,
            PasswordHash = _hasher.Hash(dto.Password),
            Role = UserRole.Prosumer,
            Status = UserStatus.Pending
        };

        await _users.InsertAsync(user);

        var p = new Prosumer
        {
            Id = Guid.NewGuid().ToString(),
            NIC = dto.NIC,
            FullName = dto.FullName,
            Email = dto.Email,
            Phone = dto.Phone,
            Address = dto.Address,
            UserId = user.Id,
            AccountStatus = UserStatus.Pending,
            IsDrpVerified = false,
            DrpVerificationRef = "",
            NicFrontImageBase64 = dto.NicFrontImageBase64,
            NicBackImageBase64 = dto.NicBackImageBase64,
            ExtractedNicNumber = extractedNic
        };

        await _pros.InsertAsync(p);
        return p;
    }

    public async Task<Prosumer?> GetByNicAsync(string nic)
    {
        // Query prosumer details by national identity card number
        var all = await _pros.GetAllAsync();
        return all.FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase));
    }

    public async Task<Prosumer> UpdateAsync(string nic, UpdateProsumerDto dto)
    {
        // Update editable profile contact fields
        var p = await GetByNicAsync(nic)
            ?? throw new KeyNotFoundException("Prosumer not found.");

        p.FullName = dto.FullName;
        p.Email = dto.Email;
        p.Phone = dto.Phone;
        p.Address = dto.Address;
        p.UpdatedAt = DateTime.UtcNow;

        await _pros.ReplaceAsync(p.Id, p);
        return p;
    }

    public async Task<Prosumer> RequestDeactivationAsync(string nic)
    {
        // Flag prosumer account with a deactivation request
        return await SetStatusAsync(nic, "deactivate-request");
    }

    public async Task<IReadOnlyList<Prosumer>> GetAllAsync()
    {
        // Retrieve all registered prosumers ordered by newest registration
        var list = await _pros.GetAllAsync();
        return list.OrderByDescending(p => p.CreatedAt).ToList();
    }

    public async Task<IReadOnlyList<Prosumer>> GetPendingAsync()
    {
        // Retrieve prosumers awaiting administrative verification
        var list = await _pros.GetAllAsync();
        return list.Where(x => x.AccountStatus == UserStatus.Pending).ToList();
    }

    public async Task<Prosumer> SetStatusAsync(string nic, string action)
    {
        // Execute administrative lifecycle status transitions (activate, reject, deactivate, etc.)
        var p = await GetByNicAsync(nic)
            ?? throw new KeyNotFoundException("Prosumer not found.");

        var user = await _users.GetByIdAsync(p.UserId)
            ?? throw new KeyNotFoundException("User not found.");

        if (action == "activate")
        {
            p.AccountStatus = UserStatus.Active;
            user.Status = UserStatus.Active;
        }
        else if (action == "deactivate" || action == "deactivate-request")
        {
            p.AccountStatus = UserStatus.Deactivated;
            user.Status = UserStatus.Deactivated;
        }
        else if (action == "reactivate")
        {
            p.AccountStatus = UserStatus.Active;
            user.Status = UserStatus.Active;
        }
        else if (action == "reject")
        {
            p.AccountStatus = UserStatus.Rejected;
            user.Status = UserStatus.Rejected;
        }
        else if (action == "verify-nic")
        {
            p.IsDrpVerified = true;
        }
        else
        {
            throw new ArgumentException("Invalid status action.");
        }

        p.UpdatedAt = DateTime.UtcNow;
        user.UpdatedAt = DateTime.UtcNow;

        await _pros.ReplaceAsync(p.Id, p);
        await _users.ReplaceAsync(user.Id, user);
        return p;
    }
}
