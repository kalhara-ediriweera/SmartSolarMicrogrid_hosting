// ============================================================================
// SmartSolar Microgrid System
// File: IEmailService.cs
// Purpose: Service contract defining operations and abstractions for Email.
// ============================================================================

namespace SmartSolarMicrogrid.Infrastructure.Interfaces;

public interface IEmailService
{
    Task SendUserWelcomeEmailAsync(string toEmail, string username, string password, string role);
    Task SendEmailAsync(string toEmail, string subject, string htmlContent, string plainTextContent = "");
}
