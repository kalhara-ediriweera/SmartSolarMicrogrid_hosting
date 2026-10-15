import { AlertTriangle, MapPin } from "lucide-react";

// Shows which branch the logged-in grid operator manages, or a warning when none is assigned.
export default function BranchNotice({ me }) {
  if (!me) return null;
  if (!me.stationId) {
    return (
      <div className="err">
        <AlertTriangle size={18} style={{ flexShrink: 0 }} />
        <div>No branch is assigned to your account. Ask the Backoffice admin to assign you a branch from User Management.</div>
      </div>
    );
  }
  return (
    <p style={{ display: "flex", alignItems: "center", gap: 8, margin: "0 0 16px", color: "#94a3b8" }}>
      <MapPin size={16} /> Branch: <strong style={{ color: "#f8fafc" }}>{me.nodeCode}</strong> {me.nodeName && <span>({me.nodeName})</span>}
    </p>
  );
}
