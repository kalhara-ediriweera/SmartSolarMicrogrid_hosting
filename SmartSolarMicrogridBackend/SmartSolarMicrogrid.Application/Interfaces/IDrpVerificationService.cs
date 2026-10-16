// ============================================================================
// SmartSolar Microgrid System
// File: IDrpVerificationService.cs
// Purpose: Service contract defining operations and abstractions for DrpVerification.
// ============================================================================

namespace SmartSolarMicrogrid.Application.Interfaces;

public record DrpVerificationResult(
    bool IsVerified,
    string ReferenceNumber,
    string? CitizenFullName,
    string? Gender,
    DateTime? DateOfBirth,
    int? Age,
    string Message
);

public interface IDrpVerificationService
{
    Task<DrpVerificationResult> VerifyNicAsync(string nic, string fullName);
}
