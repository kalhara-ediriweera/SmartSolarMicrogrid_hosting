import { api } from "./api";

export const getInternalUsers = () => api.get("/users/internal");
export const createUser = (user) => api.post("/users", user);
export const updateUser = (id, user) => api.put(`/users/${id}`, user);
export const deleteUser = (id) => api.delete(`/users/${id}`);