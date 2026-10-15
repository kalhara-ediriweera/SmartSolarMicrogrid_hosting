import { api } from "./api";

export async function login(credentials) {
  const { data } = await api.post("/auth/login", credentials);
  localStorage.setItem("token", data.token);
  localStorage.setItem("user", JSON.stringify(data));
  return data;
}

export function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    return null;
  }
}