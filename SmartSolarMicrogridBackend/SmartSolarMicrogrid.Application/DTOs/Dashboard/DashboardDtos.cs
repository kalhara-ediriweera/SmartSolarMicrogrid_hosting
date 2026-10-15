namespace SmartSolarMicrogrid.Application.DTOs.Dashboard;
public record ProsumerDashboardDto(int ActiveReservations, int PendingReservations, int HistoryCount, object? UpcomingReservation, IReadOnlyList<object> NearbyNodes);
public record OperatorDashboardDto(int PendingReservations, int ApprovedReservations, int TodayBookings, int AvailableSlots, IReadOnlyList<object> RecentTransactions);
public record BackofficeDashboardDto(int TotalProsumers, int PendingProsumerAccounts, int TotalStations, int AvailableSlots, int PendingReservations, int ApprovedReservations, int CompletedTransfers);
