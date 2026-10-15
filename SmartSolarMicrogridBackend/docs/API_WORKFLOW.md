# Core API workflow

## Prosumer
POST /api/prosumers/register
POST /api/auth/login
GET  /api/microgrid-nodes?activeOnly=true
GET  /api/energy-slots?nodeId={nodeId}&availableOnly=true
POST /api/reservations?nic={nic}
GET /api/reservations/mine?nic={nic}
PUT /api/reservations/{id}?nic={nic}
DELETE /api/reservations/{id}?nic={nic}

## Backoffice
GET /api/prosumers/pending
PUT /api/prosumers/{nic}/activate
POST /api/microgrid-nodes
PUT /api/microgrid-nodes/{id}
POST /api/energy-slots
GET /api/reservations/status/Pending
PUT /api/reservations/{id}/approve
GET /api/dashboard/backoffice

## Grid Operator
GET /api/dashboard/operator
GET /api/reservations/status/Pending
GET /api/reservations/status/Approved
PUT /api/energy-slots/{id}/availability?availableCapacityKwh=...
POST /api/qr/verify
POST /api/qr/complete

## Important implementation note
For a production app, the prosumer NIC should come from the authenticated token/session rather than a query parameter. This reference keeps the parameter explicit so the API can be tested easily from Swagger.
