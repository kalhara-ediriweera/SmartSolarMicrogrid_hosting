// ============================================================================
// SmartSolar Microgrid System
// File: ReservationValidator.cs
// Purpose: Validation rules and business integrity checks for Reservation.
// ============================================================================

namespace SmartSolarMicrogrid.Application.Validators;

// Business rules for energy reservations. "now" is passed in so the rules can be unit tested.
public static class ReservationValidator
{
    public const int BookingWindowDays = 7;
    public const int MinNoticeHours = 12;

    // A reservation can only be made for a slot between today and 7 days from today.
    public static void WithinBookingWindow(DateTime slotDate, DateTime nowUtc)
    {
        // Execute WithinBookingWindow operation logic
        if (slotDate.Date < nowUtc.Date || slotDate.Date > nowUtc.Date.AddDays(BookingWindowDays))
            throw new ArgumentException($"Reservation must be within the specified {BookingWindowDays}-day period.");
    }

    // Overload verifying slot date, slot start time, and current time.
    public static void WithinBookingWindow(DateTime slotDate, string startTime, DateTime nowUtc, DateTime? nowLocal = null)
    {
        // Execute WithinBookingWindow operation logic
        WithinBookingWindow(slotDate, nowUtc);
        var startDateTime = StartDateTime(slotDate, startTime);
        if (startDateTime <= nowUtc)
            throw new ArgumentException("Reservation slot must start in the future.");

        if (nowLocal.HasValue)
        {
            if (slotDate.Date < nowLocal.Value.Date || slotDate.Date > nowLocal.Value.Date.AddDays(BookingWindowDays))
                throw new ArgumentException($"Reservation must be within the specified {BookingWindowDays}-day period.");

            var localStart = DateTime.TryParse($"{slotDate:yyyy-MM-dd} {startTime}", out var lx) ? lx : slotDate;
            if (localStart <= nowLocal.Value)
                throw new ArgumentException("Reservation slot must start in the future.");
        }
    }

    // Modification and cancellation need at least 12 hours notice before the reservation starts.
    public static void HasMinimumNotice(DateTime reservationStartUtc, DateTime nowUtc, string action, DateTime? nowLocal = null)
    {
        // Execute HasMinimumNotice operation logic
        if (nowLocal.HasValue)
        {
            var localStart = DateTime.TryParse($"{reservationStartUtc:yyyy-MM-dd HH:mm}", out var lx) ? lx : reservationStartUtc;
            if (localStart <= nowLocal.Value.AddHours(MinNoticeHours))
                throw new InvalidOperationException($"{action} requires at least {MinNoticeHours} hours notice.");
        }

        if (reservationStartUtc <= nowUtc.AddHours(MinNoticeHours))
            throw new InvalidOperationException($"{action} requires at least {MinNoticeHours} hours notice.");
    }

    // Combines the reservation date with its "HH:mm" start time.
    public static DateTime StartDateTime(DateTime date, string time)
    {
        // Execute StartDateTime operation logic
        return DateTime.TryParse($"{date:yyyy-MM-dd} {time}", out var x) ? DateTime.SpecifyKind(x, DateTimeKind.Utc) : date;
    }
}
