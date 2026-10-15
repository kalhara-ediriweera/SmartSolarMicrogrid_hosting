import { api } from "./api";

export const getEnergySlots = () => api.get("/energy-slots");
export const createEnergySlot = (slot) => api.post("/energy-slots", slot);
export const updateEnergySlot = (id, slot) => api.put(`/energy-slots/${id}`, slot);
export const updateSlotAvailability = (id, capacity) => api.put(`/energy-slots/${id}/availability?availableCapacityKwh=${capacity}`);
export const getReservations = (status) => api.get(`/reservations/status/${status}`);
export const approveReservation = (id) => api.put(`/reservations/${id}/approve`);
export const verifyQrTransaction = (qrToken) => api.post("/qr/verify", { qrToken });
export const completeQrTransaction = (qrToken) => api.post("/qr/complete", { qrToken });