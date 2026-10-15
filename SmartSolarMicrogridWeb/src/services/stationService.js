import { api } from "./api";

export const getStations = () => api.get("/microgrid-nodes");
export const createStation = (station) => api.post("/microgrid-nodes", station);
export const updateStation = (id, station) => api.put(`/microgrid-nodes/${id}`, station);
export const deactivateStation = (id, reason) => api.put(`/microgrid-nodes/${id}/deactivate`, { reason });
export const activateStation = (id) => api.put(`/microgrid-nodes/${id}/activate`);