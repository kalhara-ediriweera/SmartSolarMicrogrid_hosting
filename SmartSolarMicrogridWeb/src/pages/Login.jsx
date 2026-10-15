import { useState } from "react";
import { Eye, EyeOff, Sun } from "lucide-react";
import { useNavigate } from "react-router-dom";
import useAuth from "../hooks/useAuth";

export default function Login() {
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();
  const { signIn } = useAuth();

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    try {
      const authenticatedUser = await signIn(form);
      navigate(authenticatedUser.role === "Backoffice" ? "/backoffice" : "/operator", { replace: true });
    } catch (requestError) {
      setError(requestError.response?.data?.error || "Login failed");
    }
  }

  return (
    <div className="login">
      <div className="loginbox">
        <div className="logo"><Sun /></div>
        <h1>Smart Solar</h1>
        <p>Microgrid Trading System</p>
        {error && <div className="err">{error}</div>}
        <form onSubmit={handleSubmit}>
          <label>Username
            <input
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
              placeholder="Enter username"
              required
            />
          </label>
          <label>Password
            <div className="password-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                value={form.password}
                onChange={(event) => setForm({ ...form, password: event.target.value })}
                placeholder="Enter password"
                required
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={showPassword ? "Hide password" : "Show password"}
                title={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </label>
          <button type="submit">Sign in</button>
        </form>
      </div>
    </div>
  );
}