namespace SmartSolarMicrogrid.Infrastructure.Interfaces;

public interface IEmailService
{
    Task SendUserWelcomeEmailAsync(string toEmail, string username, string password, string role);
    Task SendEmailAsync(string toEmail, string subject, string htmlContent, string plainTextContent = "");
}
