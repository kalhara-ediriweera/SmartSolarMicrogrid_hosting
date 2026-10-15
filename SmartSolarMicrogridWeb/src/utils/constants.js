export const USER_ROLES = Object.freeze({
  BACKOFFICE: "Backoffice",
  GRID_OPERATOR: "GridOperator",
});

export const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:5053/api";

export const RESERVATION_STATUSES = Object.freeze(["Pending", "Approved", "Completed", "Cancelled"]);