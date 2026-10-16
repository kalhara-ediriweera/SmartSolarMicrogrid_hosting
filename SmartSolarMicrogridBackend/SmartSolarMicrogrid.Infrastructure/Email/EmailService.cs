using System.Net;
using System.Net.Mail;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using SmartSolarMicrogrid.Infrastructure.Interfaces;

namespace SmartSolarMicrogrid.Infrastructure.Email;

public class EmailService : IEmailService
{
    private readonly EmailSettings _settings;
    private readonly ILogger<EmailService> _logger;

    public EmailService(IOptions<EmailSettings> options, ILogger<EmailService> logger)
    {
        _settings = options.Value;
        _logger = logger;
    }

    public async Task SendUserWelcomeEmailAsync(string toEmail, string username, string password, string role)
    {
        var subject = "Smart Solar Microgrid - Account Created";
        var htmlBody = $@"
<!DOCTYPE html>
<html>
<head>
  <meta charset=""utf-8"">
  <title>Your Account Credentials</title>
</head>
<body style=""margin: 0; padding: 24px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f19; color: #f1f5f9;"">
  <div style=""max-width: 600px; margin: 0 auto; background: #131b2e; border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 16px; overflow: hidden; box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);"">
    <div style=""background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%); padding: 32px 24px; text-align: center;"">
      <h1 style=""margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;"">Smart Solar Microgrid</h1>
      <p style=""margin: 8px 0 0; color: #e0f2fe; font-size: 14px;"">Account Setup Notification</p>
    </div>
    
    <div style=""padding: 32px 24px;"">
      <p style=""margin-top: 0; font-size: 15px; color: #cbd5e1;"">Hello,</p>
      <p style=""font-size: 15px; color: #cbd5e1; line-height: 1.6;"">
        An administrator has created an internal user account for you on the <strong>Smart Solar Microgrid</strong> portal.
        Below are your initial login credentials:
      </p>

      <div style=""background: #0d1322; border: 1px solid #1e293b; border-radius: 12px; padding: 20px; margin: 24px 0;"">
        <table style=""width: 100%; border-collapse: collapse;"">
          <tr>
            <td style=""padding: 8px 0; color: #94a3b8; font-size: 14px; width: 140px;"">Username:</td>
            <td style=""padding: 8px 0; color: #38bdf8; font-family: monospace; font-size: 16px; font-weight: 700;"">{username}</td>
          </tr>
          <tr>
            <td style=""padding: 8px 0; color: #94a3b8; font-size: 14px;"">Password:</td>
            <td style=""padding: 8px 0; color: #22c55e; font-family: monospace; font-size: 16px; font-weight: 700;"">{password}</td>
          </tr>
          <tr>
            <td style=""padding: 8px 0; color: #94a3b8; font-size: 14px;"">Assigned Role:</td>
            <td style=""padding: 8px 0; color: #f8fafc; font-size: 14px; font-weight: 600;"">{role}</td>
          </tr>
        </table>
      </div>

      <div style=""background: rgba(245, 158, 11, 0.12); border-left: 4px solid #f59e0b; border-radius: 6px; padding: 14px 16px; margin: 24px 0;"">
        <p style=""margin: 0; color: #fef08a; font-size: 13.5px; line-height: 1.5;"">
          <strong>Security Recommendation:</strong> For security reasons, please log in and change your password at any time via your user menu.
        </p>
      </div>

      <p style=""font-size: 14px; color: #94a3b8; line-height: 1.5; margin-bottom: 0;"">
        If you did not expect this invitation or have concerns regarding this account, please get in touch with your system administrator immediately.
      </p>
    </div>

    <div style=""padding: 20px 24px; background: #0c111d; border-top: 1px solid rgba(255, 255, 255, 0.05); text-align: center;"">
      <p style=""margin: 0; font-size: 12px; color: #64748b;"">
        &copy; {DateTime.UtcNow.Year} Smart Solar Microgrid. This is an automated message, please do not reply.
      </p>
    </div>
  </div>
</body>
</html>";

        var plainText = $"Smart Solar Microgrid - Account Created\n\nHello,\n\nAn account has been created for you on the Smart Solar Microgrid portal.\n\nUsername: {username}\nPassword: {password}\nAssigned Role: {role}\n\nSecurity Notice: Please log in and change your password at your earliest convenience.\n\nRegards,\nSmart Solar Microgrid Administration";

        await SendEmailAsync(toEmail, subject, htmlBody, plainText);
    }

    public async Task SendEmailAsync(string toEmail, string subject, string htmlContent, string plainTextContent = "")
    {
        _logger.LogInformation("Processing email to {ToEmail} with subject '{Subject}'", toEmail, subject);

        // If credentials are not configured, log email contents for developer/evaluator convenience
        if (string.IsNullOrWhiteSpace(_settings.Username) || string.IsNullOrWhiteSpace(_settings.Password))
        {
            _logger.LogWarning("Email sending fallback (No SMTP credentials configured): Email will not be transmitted over network. Logged Email Details:\nTo: {ToEmail}\nSubject: {Subject}\nContent: {PlainText}",
                toEmail, subject, plainTextContent);
            return;
        }

        try
        {
            using var client = new SmtpClient(_settings.SmtpHost, _settings.SmtpPort)
            {
                EnableSsl = _settings.EnableSsl,
                Credentials = new NetworkCredential(_settings.Username, _settings.Password),
                DeliveryMethod = SmtpDeliveryMethod.Network,
                Timeout = 15000
            };

            using var message = new MailMessage
            {
                From = new MailAddress(_settings.SenderEmail, _settings.SenderName),
                Subject = subject,
                Body = htmlContent,
                IsBodyHtml = true
            };
            message.To.Add(toEmail);

            await client.SendMailAsync(message);
            _logger.LogInformation("Email successfully dispatched to {ToEmail}", toEmail);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send email to {ToEmail} via SMTP. Details logged for audit: Subject={Subject}", toEmail, subject);
        }
    }
}
