// ============================================================================
// SmartSolar Microgrid System
// File: ReservationValidatorTests.cs
// Purpose: Validation rules and business integrity checks for ReservationTests.
// ============================================================================

using SmartSolarMicrogrid.Application.Validators;
using Xunit;
namespace SmartSolarMicrogrid.Tests;

public class ReservationValidatorTests
{
    private static readonly DateTime Now = new(2026, 10, 15, 8, 0, 0, DateTimeKind.Utc);

    // 7-day booking window
    [Fact] public void BookingTodayIsAllowed() => ReservationValidator.WithinBookingWindow(Now.Date, Now);
    [Fact] public void BookingOnSeventhDayIsAllowed() => ReservationValidator.WithinBookingWindow(Now.Date.AddDays(7), Now);
    [Fact]
    public void BookingAfterSevenDaysIsRejected() =>
        Assert.Throws<ArgumentException>(() => ReservationValidator.WithinBookingWindow(Now.Date.AddDays(8), Now));
    [Fact]
    public void BookingInThePastIsRejected() =>
        Assert.Throws<ArgumentException>(() => ReservationValidator.WithinBookingWindow(Now.Date.AddDays(-1), Now));

    // Start time & future validation
    [Fact]
    public void BookingFutureSlotTodayIsAllowed() =>
        ReservationValidator.WithinBookingWindow(Now.Date, "10:00", Now);
    [Fact]
    public void BookingPastSlotTimeTodayIsRejected() =>
        Assert.Throws<ArgumentException>(() => ReservationValidator.WithinBookingWindow(Now.Date, "07:00", Now));
    [Fact]
    public void BookingSlotStartingNowIsRejected() =>
        Assert.Throws<ArgumentException>(() => ReservationValidator.WithinBookingWindow(Now.Date, "08:00", Now));
    [Fact]
    public void BookingFutureSlotWithinSevenDaysIsAllowed() =>
        ReservationValidator.WithinBookingWindow(Now.Date.AddDays(5), "09:00", Now);
    [Fact]
    public void BookingFutureSlotBeyondSevenDaysIsRejected() =>
        Assert.Throws<ArgumentException>(() => ReservationValidator.WithinBookingWindow(Now.Date.AddDays(8), "09:00", Now));

    // 12-hour notice for modification / cancellation
    [Fact]
    public void ChangeWithMoreThan12HoursNoticeIsAllowed() =>
        ReservationValidator.HasMinimumNotice(Now.AddHours(13), Now, "Modification");
    [Fact]
    public void ChangeExactlyAt12HoursIsRejected() =>
        Assert.Throws<InvalidOperationException>(() => ReservationValidator.HasMinimumNotice(Now.AddHours(12), Now, "Modification"));
    [Fact]
    public void CancelWithLessThan12HoursNoticeIsRejected()
    {
        // Remove entity record and release associated allocations
        var ex = Assert.Throws<InvalidOperationException>(() => ReservationValidator.HasMinimumNotice(Now.AddHours(3), Now, "Cancellation"));
        Assert.Equal("Cancellation requires at least 12 hours notice.", ex.Message);
    }
    [Fact]
    public void ChangeWithLocalTimeLessThan12HoursNoticeIsRejected()
    {
        var localNow = new DateTime(2026, 10, 17, 1, 0, 0);
        var slotTime = new DateTime(2026, 10, 17, 8, 0, 0); // 7 hours away
        Assert.Throws<InvalidOperationException>(() =>
            ReservationValidator.HasMinimumNotice(slotTime, localNow.AddHours(-5.5), "Modification", localNow));
    }
    [Fact]
    public void ChangeWithLocalTimeMoreThan12HoursNoticeIsAllowed()
    {
        var localNow = new DateTime(2026, 10, 17, 1, 0, 0);
        var slotTime = new DateTime(2026, 10, 18, 8, 0, 0); // 31 hours away
        ReservationValidator.HasMinimumNotice(slotTime, localNow.AddHours(-5.5), "Modification", localNow);
    }

    // Reservation start time
    [Fact]
    public void StartDateTimeCombinesDateAndTime() =>
        Assert.Equal(new DateTime(2026, 10, 20, 14, 30, 0), ReservationValidator.StartDateTime(new DateTime(2026, 10, 20), "14:30"));
    [Fact]
    public void StartDateTimeFallsBackToDateForInvalidTime() =>
        Assert.Equal(new DateTime(2026, 10, 20), ReservationValidator.StartDateTime(new DateTime(2026, 10, 20), "not-a-time"));
}
