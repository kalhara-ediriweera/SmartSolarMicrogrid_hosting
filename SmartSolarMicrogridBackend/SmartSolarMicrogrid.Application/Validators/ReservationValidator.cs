namespace SmartSolarMicrogrid.Application.Validators;

// Business rules for energy reservations. "now" is passed in so the rules can be unit tested.
public static class ReservationValidator
{
    public const int BookingWindowDays = 7;
    public const int MinNoticeHours = 12;

    // A reservation can only be made for a slot between today and 7 days from today.
    public static void WithinBookingWindow(DateTime slotDate, DateTime nowUtc)
    {
        if (slotDate.Date < nowUtc.Date || slotDate.Date > nowUtc.Date.AddDays(BookingWindowDays))
            throw new ArgumentException($"Reservation must be within the specified {BookingWindowDays}-day period.");
    }

    // Modification and cancellation need at least 12 hours notice before the reservation starts.
    public static void HasMinimumNotice(DateTime reservationStartUtc, DateTime nowUtc, string action)
    {
        if (reservationStartUtc <= nowUtc.AddHours(MinNoticeHours))
            throw new InvalidOperationException($"{action} requires at least {MinNoticeHours} hours notice.");
    }

    // Combines the reservation date with its "HH:mm" start time.
    public static DateTime StartDateTime(DateTime date, string time)
    {
        return DateTime.TryParse($"{date:yyyy-MM-dd} {time}", out var x) ? DateTime.SpecifyKind(x, DateTimeKind.Utc) : date;
    }
}
