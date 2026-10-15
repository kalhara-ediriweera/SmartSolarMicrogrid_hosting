import { api } from "./api";

// Grid operator (branch-scoped)
export const getMe = () => api.get("/operator/me");
export const getDashboard = () => api.get("/operator/dashboard");
export const getSlots = () => api.get("/operator/slots");
export const getReservations = (status) => api.get(`/operator/reservations/status/${status}`);
export const approveReservation = (id) => api.put(`/operator/reservations/${id}/approve`);
export const getMySlotRequests = () => api.get("/operator/slot-requests");
export const requestSlotUnavailable = (slotId, reason) => api.post("/operator/slot-requests", { slotId, reason });

// Backoffice
export const getSlotRequests = (status) => api.get("/slot-requests", { params: status ? { status } : {} });
export const approveSlotRequest = (id, adminNote) => api.put(`/slot-requests/${id}/approve`, { adminNote });
export const rejectSlotRequest = (id, adminNote) => api.put(`/slot-requests/${id}/reject`, { adminNote });
export const getOperatorAssignments = () => api.get("/operator-assignments");
export const assignOperatorStation = (userId, stationId) => api.put(`/operator-assignments/${userId}`, { stationId });
