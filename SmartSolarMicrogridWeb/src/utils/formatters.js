export function formatDate(value, locale = undefined) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString(locale);
}

export function formatEnergy(value, unit = "kWh") {
  const amount = Number(value);
  return Number.isFinite(amount) ? `${amount.toLocaleString()} ${unit}` : "—";
}

export function formatRole(role) {
  return role === 0 || role === "Backoffice" ? "Backoffice" : "Grid Operator";
}