import { Battery, CalendarDays, LayoutDashboard, LogOut, MapPin, QrCode, Sun, UserPlus, Users, X } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import useAuth from "../../hooks/useAuth";

const links = {
  Backoffice: [
    ["/backoffice", "Dashboard", LayoutDashboard],
    ["/backoffice/users", "User Management", UserPlus],
    ["/backoffice/prosumers", "Prosumer Management", Users],
    ["/backoffice/stations", "Microgrid Nodes", MapPin],
    ["/backoffice/slots", "Energy Slots", Battery],
    ["/backoffice/reservations", "Reservations", CalendarDays],
  ],
  GridOperator: [
    ["/operator", "Dashboard", LayoutDashboard],
    ["/operator/bookings", "Bookings", CalendarDays],
    ["/operator/availability", "Availability", Battery],
    ["/operator/qr", "QR Verification", QrCode],
  ],
};

export default function Sidebar({ role, mobileOpen, collapsed, onClose }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { signOut } = useAuth();

  function handleSignOut() {
    signOut();
    navigate("/login", { replace: true });
  }

  return (
    <>
      <aside className={`side ${mobileOpen ? "open" : ""} ${collapsed ? "collapsed" : ""}`}>
        <div className="brand">
          <div className="brand-text">
            <Sun size={26} />
            <span className="brand-title">Smart Solar</span>
          </div>
          <button className="side-close" onClick={onClose} aria-label="Close menu">
            <X size={20} />
          </button>
        </div>
        {(links[role] || []).map(([path, title, Icon]) => (
          <button
            className={location.pathname === path ? "nav active" : "nav"}
            onClick={() => { navigate(path); onClose(); }}
            key={path}
            title={title}
          >
            <Icon size={20} className="nav-icon" />
            <span className="nav-text">{title}</span>
          </button>
        ))}
        <button className="nav bottom" onClick={handleSignOut} title="Logout">
          <LogOut size={20} className="nav-icon" />
          <span className="nav-text">Logout</span>
        </button>
      </aside>
      {mobileOpen && <div className="overlay" onClick={onClose} />}
    </>
  );
}