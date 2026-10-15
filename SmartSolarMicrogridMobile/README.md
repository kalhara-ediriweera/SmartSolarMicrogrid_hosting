# Smart Solar Microgrid Trading System – Native Android Client

Pure native Android Java client for the SE4040 Smart Solar Microgrid Trading System.

## Assignment-aligned mobile features

### Prosumer
- Login with role-based routing
- Native registration using NIC as primary identifier
- View/update own profile
- Request account deactivation
- Dashboard with active, pending and completed reservation counts
- View available energy slots
- Create reservation
- Seven-day reservation rule is enforced by the C# API
- Modify pending/approved reservation; 12-hour rule is enforced by API
- Cancel reservation; 12-hour rule is enforced by API
- Booking history
- Booking search and status filter
- Generate secure transaction QR after server approval
- Display transaction QR to Grid Operator
- Nearby microgrid nodes on Google Maps using server-stored GPS coordinates
- Station details on map marker
- SQLite local persistence for required user/reference information

### Grid Operator
- Login with role-based routing
- Operational dashboard
- Pending reservations
- Approved reservations
- Booking details
- View available energy slots
- Update battery/energy slot availability
- Native QR scanner using device camera
- Server-side QR verification
- Finalize verified energy transfer

## Backend endpoints used

Base path: `/api/`

- POST `auth/login`
- POST `prosumers/register`
- GET `prosumers/{nic}`
- PUT `prosumers/{nic}`
- POST `prosumers/{nic}/deactivation-request`
- GET `dashboard/prosumer?nic=...`
- GET `dashboard/operator`
- GET `microgrid-nodes?activeOnly=true`
- GET `energy-slots?availableOnly=true`
- POST `reservations?nic=...`
- GET `reservations/mine?nic=...&search=...&status=...`
- PUT `reservations/{id}?nic=...`
- DELETE `reservations/{id}?nic=...`
- POST `qr/generate/{reservationId}`
- POST `qr/verify`
- POST `qr/complete`
- GET `reservations/status/{status}`
- PUT `energy-slots/{id}/availability?availableCapacityKwh=...`

## Configure API URL

Edit:
`app/src/main/java/com/smartsenergy/microgrid/utils/SessionManager.java`

Change:
`BASE_URL`

a) Android Emulator talking to a local PC Kestrel server:
`http://10.0.2.2:PORT/api/`

b) Physical Android phone talking to a PC on the same Wi-Fi:
`http://YOUR_PC_LAN_IP:PORT/api/`

c) IIS HTTPS deployment:
`https://YOUR_SERVER_HOST/api/`

Use the actual port/host exposed by your C# API. Do not leave the placeholder URL for a real run.

## Google Maps

1. Create/configure a Google Maps Android API key.
2. Enable Maps SDK for Android.
3. Replace `YOUR_GOOGLE_MAPS_API_KEY` in `app/src/main/AndroidManifest.xml`.
4. The app plots active nodes using latitude/longitude returned by the C# API.

## Android Studio

Open the `SmartSolarMicrogridMobile` folder in Android Studio and let Gradle sync.

Recommended environment:
- Android Studio recent stable version
- JDK 17
- Android SDK 35
- Minimum Android 7.0 (API 24)

The project is Java-only and does not use Flutter, React Native, Kotlin Multiplatform, or another cross-platform UI framework.

## Important backend compatibility note

This mobile client is intentionally written against the controllers and DTOs in the supplied SmartSolarMicrogrid C# reference backend. The current backend uses `nic` as a query parameter for Prosumer dashboard/reservation operations, so the mobile client sends that parameter.

The server remains the authority for all business rules. The Android app does not connect to MongoDB directly.

## Demo flow

### Prosumer
Register → Backoffice activates account → Login → Dashboard → choose available slot → create booking → Backoffice approves → My Bookings → Get QR → show QR at station.

### Grid Operator
Login → Dashboard → Scan Prosumer QR → server verifies reservation → Finalize Transfer → reservation becomes Completed.

## Build

Open in Android Studio and run the `app` configuration on an emulator or physical device.

The generated package intentionally does not include Gradle wrapper binaries because this environment cannot download the Gradle distribution. Android Studio can generate/sync the wrapper from the project configuration.
