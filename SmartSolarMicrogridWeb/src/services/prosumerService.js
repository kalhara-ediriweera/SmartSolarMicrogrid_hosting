import { api } from "./api";

export const getProsumers = () => api.get("/prosumers/all");
export const updateProsumerStatus = (nic, action) => api.put(`/prosumers/${nic}/${action}`);