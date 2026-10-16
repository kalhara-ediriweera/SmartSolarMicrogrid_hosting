namespace SmartSolarMicrogrid.Infrastructure.Email;

public class EmailSettings
{
    public string SmtpHost { get; set; } = "smtp.gmail.com";
    public int SmtpPort { get; set; } = 587;
    public bool EnableSsl { get; set; } = true;
    public string SenderEmail { get; set; } = "noreply@smartsolarmicrogrid.com";
    public string SenderName { get; set; } = "Smart Solar Microgrid Admin";
    public string Username { get; set; } = "";
    public string Password { get; set; } = "";
}
