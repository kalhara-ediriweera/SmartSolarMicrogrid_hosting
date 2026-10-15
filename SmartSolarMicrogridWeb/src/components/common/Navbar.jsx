import { Menu } from "lucide-react";
import useAuth from "../../hooks/useAuth";

export default function Navbar({ collapsed, onToggle }) {
  const { currentUser } = useAuth();

  return (
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
      <b className="user-badge">{currentUser?.username}</b>
    </header>
  );
}