export function formatDate(value, locale = undefined) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString(locale);
}

export function formatEnergy(value, unit = "kWh") {
  if (value == null || value === "" || isNaN(value)) return "—";
  const amount = Number(value);
  if (!Number.isFinite(amount)) return "—";
  const formatted = Number.isInteger(amount) ? amount.toString() : Number(amount.toFixed(3)).toString();
  return `${formatted} ${unit}`;
}

export function formatRole(role) {
  return role === 0 || role === "Backoffice" ? "Backoffice" : "Grid Operator";
}