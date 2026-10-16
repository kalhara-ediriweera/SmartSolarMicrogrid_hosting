import { useEffect, useState } from "react";
import { 
  Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, 
  ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, 
  Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle,
  Sparkles, Copy, Mail, KeyRound, RefreshCw, Send
} from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as userService from "../../services/userService";
import OperatorBranchAssignments from "../../components/backoffice/OperatorBranchAssignments";
import PasswordStrengthMatrix from "../../components/common/PasswordStrengthMatrix";
import { generateStrongPassword, evaluatePasswordStrength } from "../../utils/passwordUtils";

export default function UserMgmt() {
  const empty = { username: "", email: "", password: "", confirmPassword: "", role: 1, status: 0 };
  const [f, setF] = useState(() => {
    const initialPass = generateStrongPassword(14);
    return { ...empty, password: initialPass, confirmPassword: initialPass };
  });
  const [a, setA] = useState([]);
  const [e, setE] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [createdCredentials, setCreatedCredentials] = useState(null);
  const [filter, setFilter] = useState("All Roles");
  const [edit, setEdit] = useState(null);
  const [showPassword, setShowPassword] = useState(true);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [copied, setCopied] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const load = () => userService.getInternalUsers().then((x) => setA(x.data));

  useEffect(() => {
    load();
  }, []);

  function handleAutoGeneratePassword() {
    const newPass = generateStrongPassword(14);
    setF((prev) => ({ ...prev, password: newPass, confirmPassword: newPass }));
    setShowPassword(true);
    setCopied(false);
  }

  function handleCopyPassword() {
    if (!f.password) return;
    navigator.clipboard.writeText(f.password);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  function resetForm() {
    const newPass = generateStrongPassword(14);
    setEdit(null);
    setF({ ...empty, password: newPass, confirmPassword: newPass });
    setE("");
    setShowPassword(true);
    setCopied(false);
  }

  async function saveF(x) {
    x.preventDefault();
    setE("");
    setSuccessMsg("");
    setCreatedCredentials(null);

    // Validate email format
    if (!f.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) {
      return setE("Please enter a valid email address.");
    }

    // Password validation
    if (!edit || f.password) {
      if (f.password !== f.confirmPassword) {
        return setE("Passwords do not match.");
      }
      const strength = evaluatePasswordStrength(f.password);
      if (!strength.isValid) {
        return setE("Password does not meet all strong password requirements.");
      }
    }

    try {
      setSubmitting(true);
      if (edit) {
        await userService.updateUser(edit, {
          email: f.email.trim(),
          role: +f.role,
          status: +f.status,
          ...(f.password ? { password: f.password } : {}),
        });
        setSuccessMsg("User updated successfully!");
        resetForm();
      } else {
        const createdUser = await userService.createUser({
          username: f.username.trim(),
          email: f.email.trim(),
          password: f.password,
          role: +f.role,
          status: +f.status,
        });

        setSuccessMsg(`User created successfully! Welcome email with username and password dispatched to ${f.email}.`);
        setCreatedCredentials({
          username: f.username.trim(),
          email: f.email.trim(),
          password: f.password,
          role: +f.role === 0 ? "Backoffice" : "Grid Operator",
        });

        resetForm();
      }
      load();
    } catch (err) {
      setE(err.response?.data?.error || "Failed to save user.");
    } finally {
      setSubmitting(false);
    }
  }

  async function del(id) {
    if (window.confirm("Are you sure you want to delete this user?")) {
      try {
        await userService.deleteUser(id);
        load();
      } catch (err) {
        setE(err.response?.data?.error || "Failed to delete user.");
      }
    }
  }

  function startEdit(u) {
    setEdit(u.id);
    setF({
      username: u.username,
      email: u.email || "",
      password: "",
      confirmPassword: "",
      role: u.role,
      status: u.status,
    });
    setE("");
    setSuccessMsg("");
    setCreatedCredentials(null);
    setShowPassword(false);
  }

  const fa = filter === "All Roles" ? a : a.filter((u) => u.role === (filter === "Backoffice" ? 0 : 1));

  return (
    <>
      <Title
        t="User Management"
        d="Create, monitor, and manage Backoffice administrators and Grid Operator accounts"
      />

      <div className="grid2">
        {/* Create / Edit User Form */}
        <form className="card" onSubmit={saveF}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
            <h2 style={{ margin: 0, display: "flex", alignItems: "center", gap: "8px" }}>
              <UserPlus size={20} color="#0A84FF" />
              {edit ? "Edit User" : "Create User"}
            </h2>
            {!edit && (
              <span
                style={{
                  fontSize: "12px",
                  color: "#10b981",
                  background: "rgba(16, 185, 129, 0.12)",
                  padding: "4px 8px",
                  borderRadius: "6px",
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "4px",
                  fontWeight: 600,
                }}
              >
                <Sparkles size={13} /> Auto-Strong Password
              </span>
            )}
          </div>

          {e && (
            <div className="err" style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "14px" }}>
              <AlertCircle size={16} style={{ flexShrink: 0 }} />
              <span>{e}</span>
            </div>
          )}

          {successMsg && (
            <div
              style={{
                background: "rgba(16, 185, 129, 0.15)",
                border: "1px solid rgba(16, 185, 129, 0.3)",
                color: "#6ee7b7",
                padding: "10px 14px",
                borderRadius: "8px",
                fontSize: "13px",
                marginBottom: "14px",
                display: "flex",
                alignItems: "center",
                gap: "8px",
              }}
            >
              <CheckCircle2 size={16} color="#10b981" style={{ flexShrink: 0 }} />
              <span>{successMsg}</span>
            </div>
          )}

          {createdCredentials && (
            <div
              style={{
                background: "rgba(10, 132, 255, 0.1)",
                border: "1px solid rgba(10, 132, 255, 0.3)",
                borderRadius: "10px",
                padding: "14px",
                marginBottom: "16px",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "#38bdf8", fontWeight: 600, fontSize: "13px", marginBottom: "8px" }}>
                <Send size={15} /> Credentials Emailed to {createdCredentials.email}
              </div>
              <div style={{ fontSize: "12.5px", color: "#cbd5e1", lineHeight: "1.6" }}>
                <div><strong>Username:</strong> <code style={{ color: "#38bdf8" }}>{createdCredentials.username}</code></div>
                <div><strong>Password:</strong> <code style={{ color: "#22c55e" }}>{createdCredentials.password}</code></div>
                <div><strong>Role:</strong> {createdCredentials.role}</div>
              </div>
            </div>
          )}

          <label>
            Username
            <input
              required={!edit}
              disabled={!!edit}
              placeholder="e.g. jsmith"
              value={f.username}
              onChange={(e) => setF({ ...f, username: e.target.value })}
            />
          </label>

          <label>
            Email Address
            <input
              required
              type="email"
              placeholder="user@smartsolarmicrogrid.com"
              value={f.email}
              onChange={(e) => setF({ ...f, email: e.target.value })}
            />
          </label>

          <label>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span>{edit ? "Change Password (optional)" : "Password"}</span>
              <button
                type="button"
                onClick={handleAutoGeneratePassword}
                style={{
                  background: "rgba(10, 132, 255, 0.15)",
                  border: "1px solid rgba(10, 132, 255, 0.3)",
                  color: "#38bdf8",
                  padding: "3px 8px",
                  borderRadius: "6px",
                  fontSize: "11.5px",
                  fontWeight: 600,
                  cursor: "pointer",
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "4px",
                }}
                title="Generate a cryptographically strong password"
              >
                <RefreshCw size={12} /> Generate Strong
              </button>
            </div>
            <div style={{ position: "relative" }}>
              <input
                required={!edit}
                type={showPassword ? "text" : "password"}
                value={f.password}
                placeholder={edit ? "Leave blank to keep current password" : "Enter or auto-generate strong password"}
                onChange={(e) => setF({ ...f, password: e.target.value })}
                style={{ paddingRight: "70px", fontFamily: showPassword ? "monospace" : "inherit" }}
              />
              <div
                style={{
                  position: "absolute",
                  right: "8px",
                  top: "50%",
                  transform: "translateY(-50%)",
                  display: "flex",
                  gap: "4px",
                }}
              >
                {f.password && (
                  <button
                    type="button"
                    onClick={handleCopyPassword}
                    style={{
                      background: "transparent",
                      border: "none",
                      color: copied ? "#10b981" : "var(--text-muted)",
                      cursor: "pointer",
                      padding: "4px",
                    }}
                    title={copied ? "Copied!" : "Copy Password"}
                  >
                    {copied ? <Check size={16} /> : <Copy size={16} />}
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    background: "transparent",
                    border: "none",
                    color: "var(--text-muted)",
                    cursor: "pointer",
                    padding: "4px",
                  }}
                  title={showPassword ? "Hide Password" : "Show Password"}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>
          </label>

          {/* Show Strong Password Matrix whenever a password is being typed or generated */}
          {(!edit || f.password) && (
            <PasswordStrengthMatrix password={f.password} showDetails={true} />
          )}

          <label>
            Confirm Password
            <div style={{ position: "relative" }}>
              <input
                required={!edit && !!f.password}
                type={showConfirmPassword ? "text" : "password"}
                value={f.confirmPassword}
                placeholder={edit ? "Confirm new password" : "Re-enter password to confirm"}
                onChange={(e) => setF({ ...f, confirmPassword: e.target.value })}
                style={{ paddingRight: "40px" }}
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                style={{
                  position: "absolute",
                  right: "10px",
                  top: "50%",
                  transform: "translateY(-50%)",
                  background: "transparent",
                  border: "none",
                  color: "var(--text-muted)",
                  cursor: "pointer",
                  padding: "4px",
                }}
                title={showConfirmPassword ? "Hide Password" : "Show Password"}
              >
                {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {f.password && f.confirmPassword && f.password !== f.confirmPassword && (
              <span style={{ fontSize: "12px", color: "#f87171", marginTop: "4px", display: "block" }}>
                Passwords do not match
              </span>
            )}
          </label>

          <label>
            Role
            <select value={f.role} onChange={(e) => setF({ ...f, role: e.target.value })}>
              <option value={0}>Backoffice</option>
              <option value={1}>Grid Operator</option>
            </select>
          </label>

          <label>
            Status
            <select value={f.status} onChange={(e) => setF({ ...f, status: e.target.value })}>
              <option value={0}>Active</option>
              <option value={1}>Inactive</option>
            </select>
          </label>

          <div className="form-actions">
            <button
              type="submit"
              disabled={submitting || (!edit && !evaluatePasswordStrength(f.password).isValid)}
              style={{
                flex: 1,
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "8px",
              }}
            >
              {submitting && <Loader2 size={16} className="spin" />}
              {edit ? "Update User" : "Create User & Dispatch Email"}
            </button>
            {edit && (
              <button
                type="button"
                onClick={resetForm}
                style={{ background: "#94a3b8", flex: 1 }}
              >
                Cancel
              </button>
            )}
          </div>
        </form>

        {/* Retrieve Section: Internal Users Table */}
        <Table>
          <div className="table-header">
            <h2 style={{ margin: 0, display: "flex", alignItems: "center", gap: "8px" }}>
              <Users size={20} color="#0A84FF" />
              Internal Users ({fa.length})
            </h2>
            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              style={{
                padding: "6px 12px",
                borderRadius: "8px",
                border: "1px solid #cbd5e1",
                width: "auto",
                marginTop: 0,
                fontWeight: 600,
                color: "#475569",
                cursor: "pointer",
              }}
            >
              <option>All Roles</option>
              <option>Backoffice</option>
              <option>Grid Operator</option>
            </select>
          </div>

          <table>
            <thead>
              <tr>
                <th>Username</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {fa.length > 0 ? (
                fa.map((u) => (
                  <tr key={u.id}>
                    <td style={{ fontWeight: 600, color: "#f8fafc" }}>{u.username}</td>
                    <td style={{ color: "#38bdf8" }}>
                      {u.email ? (
                        <span style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}>
                          <Mail size={13} color="#94a3b8" />
                          {u.email}
                        </span>
                      ) : (
                        <span style={{ color: "#64748b", fontStyle: "italic" }}>No email set</span>
                      )}
                    </td>
                    <td>
                      <span
                        style={{
                          display: "inline-block",
                          padding: "3px 8px",
                          borderRadius: "6px",
                          fontSize: "12px",
                          fontWeight: 600,
                          background: u.role === 0 ? "rgba(10, 132, 255, 0.15)" : "rgba(168, 85, 247, 0.15)",
                          color: u.role === 0 ? "#38bdf8" : "#c084fc",
                        }}
                      >
                        {u.role === 0 ? "Backoffice" : "Grid Operator"}
                      </span>
                    </td>
                    <td>
                      <span
                        style={{
                          display: "inline-flex",
                          alignItems: "center",
                          gap: "5px",
                          fontSize: "12px",
                          fontWeight: 600,
                          color: u.status === 0 ? "#4ade80" : "#f87171",
                        }}
                      >
                        <span
                          style={{
                            width: "7px",
                            height: "7px",
                            borderRadius: "50%",
                            background: u.status === 0 ? "#4ade80" : "#f87171",
                          }}
                        />
                        {u.status === 0 ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td style={{ fontSize: "13px", color: "#94a3b8" }}>
                      {new Date(u.createdAt).toLocaleDateString()}
                    </td>
                    <td>
                      <button className="btn-edit" onClick={() => startEdit(u)}>
                        Edit
                      </button>
                      <button className="btn-delete" onClick={() => del(u.id)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" style={{ textAlign: "center", padding: "24px", color: "#64748b" }}>
                    No users found for this role filter.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Table>
      </div>

      <OperatorBranchAssignments />
    </>
  );
}
