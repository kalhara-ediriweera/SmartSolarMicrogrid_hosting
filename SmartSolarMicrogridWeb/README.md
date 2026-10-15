# Smart Solar Microgrid Web
React + Tailwind CSS client for the C# REST API.

## Run
1. Copy `.env.example` to `.env` and set `VITE_API_URL` to the API base URL.
2. `npm install`
3. `npm run dev`
4. Open the Vite URL.

Vite serves the root `index.html`; the React entry is `src/main.jsx`. Keep the HTML entry at the project root rather than moving it into `public/`.

## Source Layout
- `src/components/common/`: navigation, access control, and shared UI.
- `src/components/dashboard/`: dashboard widgets.
- `src/context/` and `src/hooks/`: authentication state and hooks.
- `src/pages/` and `src/routes/`: login, error pages, and route registration.
- `src/services/`: Axios client, authentication, and backend endpoint modules.
- `src/utils/`: shared constants, validation, and formatting helpers.

## Web scope
Backoffice: dashboard, Prosumer management, station/node management, energy slots, reservation monitoring/approval.
Grid Operator: dashboard, bookings, availability, QR verification and transfer completion.

The assignment requires the web client to communicate with the central REST API rather than MongoDB directly. Prosumer registration, booking UI, Google Maps, and QR camera scanning belong to the native Android application.
