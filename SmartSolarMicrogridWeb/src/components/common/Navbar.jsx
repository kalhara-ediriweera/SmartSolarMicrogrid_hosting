import { useState } from "react";
import { Menu, KeyRound, Shield } from "lucide-react";
import useAuth from "../../hooks/useAuth";
import ChangePasswordModal from "./ChangePasswordModal";

export default function Navbar({ collapsed, onToggle }) {
  const { currentUser } = useAuth();
  const [showPasswordModal, setShowPasswordModal] = useState(false);

  return (
    <>
      <header>
        <div className="header-left">
          <button
            className="hamb"
            onClick={onToggle}
            aria-label={collapsed ? "Expand sidebar" : "Minimize sidebar"}
            title={collapsed ? "Expand sidebar" : "Minimize sidebar"}
          >
            <Menu size={20} />
          </button>
          <span className="header-title">Smart Solar Microgrid</span>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
          <button
            type="button"
            onClick={() => setShowPasswordModal(true)}
            style={{
              background: "rgba(10, 132, 255, 0.12)",
              border: "1px solid rgba(10, 132, 255, 0.25)",
              color: "#38bdf8",
              padding: "6px 12px",
              borderRadius: "8px",
              fontSize: "13px",
              fontWeight: 500,
              cursor: "pointer",
              display: "inline-flex",
              alignItems: "center",
              gap: "6px",
              transition: "all 0.2s ease",
            }}
            title="Change your account password at any time"
          >
            <KeyRound size={15} />
            <span>Change Password</span>
          </button>

          <b className="user-badge" title={`Logged in as ${currentUser?.username}`}>
            {currentUser?.username}
          </b>
        </div>
      </header>

      <ChangePasswordModal
        isOpen={showPasswordModal}
        onClose={() => setShowPasswordModal(false)}
      />
    </>
  );
}