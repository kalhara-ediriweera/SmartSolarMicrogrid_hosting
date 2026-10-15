import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import StatCard from "../../components/dashboard/StatCard";
import { api } from "../../services/api";

export default function BOHome() {
  const [d, setD] = useState({});
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const loadData = () => {
    setLoading(true);
    setErr("");
    api.get("/dashboard/backoffice")
      .then((x) => {
        setD(x.data || {});
        setLoading(false);
      })
      .catch((x) => {
        setErr(x.response?.data?.error || "Failed to load dashboard data. Please try again.");
        setLoading(false);
      });
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <>
      <Title t="Backoffice Dashboard" d="Administration and microgrid overview" />
      {err && (
        <div className="err" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
          <span>{err}</span>
          <button type="button" onClick={loadData} style={{ padding: "4px 10px", fontSize: "12px", background: "rgba(239, 68, 68, 0.2)", border: "1px solid rgba(239, 68, 68, 0.4)", color: "#fca5a5", borderRadius: "4px", cursor: "pointer" }}>Retry</button>
        </div>
      )}
      <div className="stats">
        <StatCard t="Active Prosumers" v={loading ? "..." : d.totalProsumers} I={Users} />
        <StatCard t="Pending Accounts" v={loading ? "..." : d.pendingProsumerAccounts} I={ShieldCheck} />
        <StatCard t="Microgrid Nodes" v={loading ? "..." : d.totalStations} I={MapPin} />
        <StatCard t="Available Slots" v={loading ? "..." : d.availableSlots} I={Battery} />
        <StatCard t="Pending Reservations" v={loading ? "..." : d.pendingReservations} I={CalendarDays} />
        <StatCard t="Approved Reservations" v={loading ? "..." : d.approvedReservations} I={Activity} />
        <StatCard t="Completed Transfers" v={loading ? "..." : d.completedTransfers} I={QrCode} />
      </div>
    </>
  );
}
