using System.Text.RegularExpressions;

namespace SmartSolarMicrogrid.Application.Validators;

public static class UserValidator
{
    private static readonly Regex EmailRegex = new(
        @"^[^@\s]+@[^@\s]+\.[^@\s]+$",
        RegexOptions.Compiled | RegexOptions.IgnoreCase);

    public static void ValidateEmail(string? email)
    {
        if (string.IsNullOrWhiteSpace(email))
            throw new ArgumentException("Email address is required.");

        if (!EmailRegex.IsMatch(email.Trim()))
            throw new ArgumentException("Invalid email address format.");
    }

    public static void ValidatePassword(string? password)
    {
        if (string.IsNullOrWhiteSpace(password))
            throw new ArgumentException("Password is required.");

        if (password.Length < 8)
            throw new ArgumentException("Password must be at least 8 characters long.");

        if (!Regex.IsMatch(password, @"[A-Z]"))
            throw new ArgumentException("Password must contain at least one uppercase letter (A-Z).");

        if (!Regex.IsMatch(password, @"[a-z]"))
            throw new ArgumentException("Password must contain at least one lowercase letter (a-z).");

        if (!Regex.IsMatch(password, @"[0-9]"))
            throw new ArgumentException("Password must contain at least one number (0-9).");

        if (!Regex.IsMatch(password, @"[!@#$%^&*()_+\-=\[\]{};':""\\|,.<>\/?]"))
            throw new ArgumentException("Password must contain at least one special character (!@#$%^&* etc.).");
    }
}
