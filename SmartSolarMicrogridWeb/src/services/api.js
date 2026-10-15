import axios from "axios";
import { API_BASE_URL } from "../utils/constants";

export const api = axios.create({
  baseURL: API_BASE_URL,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  const isLoginRequest = config.url?.replace(/^\//, "") === "auth/login";
  if (token && !isLoginRequest) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const user = () => JSON.parse(localStorage.getItem("user") || "null");
