using SmartSolarMicrogrid.Application.Validators;
using Xunit;
namespace SmartSolarMicrogrid.Tests;

public class ReservationValidatorTests
{
    private static readonly DateTime Now = new(2026, 10, 15, 8, 0, 0, DateTimeKind.Utc);

    // 7-day booking window
    [Fact] public void BookingTodayIsAllowed() => ReservationValidator.WithinBookingWindow(Now.Date, Now);
    [Fact] public void BookingOnSeventhDayIsAllowed() => ReservationValidator.WithinBookingWindow(Now.Date.AddDays(7), Now);
    [Fact] public void BookingAfterSevenDaysIsRejected() =>
        Assert.Throws<ArgumentException>(() => ReservationValidator.WithinBookingWindow(Now.Date.AddDays(8), Now));
    [Fact] public void BookingInThePastIsRejected() =>
        Assert.Throws<ArgumentException>(() => ReservationValidator.WithinBookingWindow(Now.Date.AddDays(-1), Now));

    // 12-hour notice for modification / cancellation
    [Fact] public void ChangeWithMoreThan12HoursNoticeIsAllowed() =>
        ReservationValidator.HasMinimumNotice(Now.AddHours(13), Now, "Modification");
    [Fact] public void ChangeExactlyAt12HoursIsRejected() =>
        Assert.Throws<InvalidOperationException>(() => ReservationValidator.HasMinimumNotice(Now.AddHours(12), Now, "Modification"));
    [Fact] public void CancelWithLessThan12HoursNoticeIsRejected()
    {
        var ex = Assert.Throws<InvalidOperationException>(() => ReservationValidator.HasMinimumNotice(Now.AddHours(3), Now, "Cancellation"));
        Assert.Equal("Cancellation requires at least 12 hours notice.", ex.Message);
    }

    // Reservation start time
    [Fact] public void StartDateTimeCombinesDateAndTime() =>
        Assert.Equal(new DateTime(2026, 10, 20, 14, 30, 0), ReservationValidator.StartDateTime(new DateTime(2026, 10, 20), "14:30"));
    [Fact] public void StartDateTimeFallsBackToDateForInvalidTime() =>
        Assert.Equal(new DateTime(2026, 10, 20), ReservationValidator.StartDateTime(new DateTime(2026, 10, 20), "not-a-time"));
}
