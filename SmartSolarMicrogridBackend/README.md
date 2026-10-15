# Smart Solar Microgrid Trading System — Backend Reference

This is a complete C# REST API reference/starter implementation for the assignment requirements:
- Role-based authentication: Backoffice, GridOperator, Prosumer
- Prosumer lifecycle and NIC identification
- Microgrid node/station CRUD and schedules
- Energy slot CRUD and availability
- Reservation create/update/cancel, 7-day and 12-hour rules
- Pending/approved/cancelled/completed reservation workflow
- QR generation/verification/completion
- Prosumer, Grid Operator and Backoffice dashboard APIs
- Google Maps-ready node location data
- MongoDB server-side persistence
- JWT authorization
- Centralized business logic and validation

IMPORTANT:
1. This is a reference implementation to study, test and adapt. Do not submit it unchanged as your own work.
2. The assignment material should be the authority for your final implementation and report.
3. Reservation approval is implemented as a Backoffice workflow because the functional requirements define pending/approved states and QR generation after approval, but do not explicitly name the approver. Document this as your team's design decision if you keep it.
4. No API can honestly guarantee "full marks"; marks depend on the final integrated system, UI, testing, report, demo and lecturer rubric.

## Requirements
- .NET 8 SDK
- MongoDB 6/7/8
- Visual Studio 2022 / Rider / VS Code

## Run
1. Start MongoDB locally.
2. Open `SmartSolarMicrogrid.sln`.
3. Restore packages.
4. Run `SmartSolarMicrogrid.API`.
5. Swagger: `/swagger`
6. Default MongoDB:
   mongodb://localhost:27017
7. Database:
   SmartSolarMicrogridDB

## Seed
The API creates demo users on first start:
- Backoffice: `admin` / `Admin@123`
- Grid Operator: `operator1` / `Operator@123`
- Prosumer: `prosumer1` / `Prosumer@123`

Change these passwords before any real deployment.

## Main workflow
Prosumer register/login -> Backoffice activates account -> Backoffice creates station and slots -> Prosumer books slot -> Backoffice approves reservation -> API generates secure QR -> Prosumer displays QR -> Grid Operator scans/verifies -> Grid Operator completes transfer.

## Architecture
Web UI / Android UI
        |
        v
     REST API
        |
  Business Services
        |
  MongoDB Repositories
        |
     MongoDB

Android may additionally keep required local SQLite data; it must not directly access server MongoDB.
