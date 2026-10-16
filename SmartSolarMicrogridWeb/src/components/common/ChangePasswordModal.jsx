import { useState } from "react";
import { X, Lock, KeyRound, Eye, EyeOff, Sparkles, CheckCircle2, AlertCircle, Loader2 } from "lucide-react";
import { changePassword } from "../../services/userService";
import { generateStrongPassword, evaluatePasswordStrength } from "../../utils/passwordUtils";
import PasswordStrengthMatrix from "./PasswordStrengthMatrix";

export default function ChangePasswordModal({ isOpen, onClose }) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  if (!isOpen) return null;

  const strength = evaluatePasswordStrength(newPassword);
  const passwordsMatch = newPassword && confirmPassword && newPassword === confirmPassword;

  function handleAutoGenerate() {
    const generated = generateStrongPassword(14);
    setNewPassword(generated);
    setConfirmPassword(generated);
    setShowNew(true);
    setShowConfirm(true);
    setError("");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!currentPassword) {
      return setError("Please enter your current password.");
    }

    if (!strength.isValid) {
      return setError("New password does not meet all strong password criteria.");
    }

    if (newPassword !== confirmPassword) {
      return setError("New passwords do not match.");
    }

    try {
      setLoading(true);
      await changePassword({
        currentPassword,
        newPassword,
      });

      setSuccess("Your password has been changed successfully!");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");

      setTimeout(() => {
        onClose();
        setSuccess("");
      }, 1800);
    } catch (err) {
      setError(err.response?.data?.error || "Failed to update password. Please check your current password.");
    } finally {
      setLoading(false);
    }
  }

  function handleClose() {
    if (loading) return;
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setError("");
    setSuccess("");
    onClose();
  }

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div
        className="modal-card"
        style={{ maxWidth: "480px" }}
        role="dialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h2 style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "18px" }}>
            <KeyRound size={20} color="#0A84FF" />
            Change Password
          </h2>
          <button className="modal-close-btn" type="button" onClick={handleClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>

        {error && (
          <div
            style={{
              background: "rgba(239, 68, 68, 0.15)",
              border: "1px solid rgba(239, 68, 68, 0.3)",
              color: "#fca5a5",
              padding: "10px 14px",
              borderRadius: "8px",
              fontSize: "13px",
              marginBottom: "16px",
              display: "flex",
              alignItems: "center",
              gap: "8px",
            }}
          >
            <AlertCircle size={16} color="#ef4444" style={{ flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div
            style={{
              background: "rgba(16, 185, 129, 0.15)",
              border: "1px solid rgba(16, 185, 129, 0.3)",
              color: "#6ee7b7",
              padding: "10px 14px",
              borderRadius: "8px",
              fontSize: "13px",
              marginBottom: "16px",
              display: "flex",
              alignItems: "center",
              gap: "8px",
            }}
          >
            <CheckCircle2 size={16} color="#10b981" style={{ flexShrink: 0 }} />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* Current Password */}
          <div style={{ marginBottom: "14px" }}>
            <label style={{ display: "block", fontSize: "13px", color: "var(--text-muted)", marginBottom: "6px" }}>
              Current Password
            </label>
            <div style={{ position: "relative" }}>
              <input
                required
                type={showCurrent ? "text" : "password"}
                value={currentPassword}
                placeholder="Enter current password"
                onChange={(e) => setCurrentPassword(e.target.value)}
                style={{
                  width: "100%",
                  paddingRight: "40px",
                  background: "rgba(255, 255, 255, 0.04)",
                  border: "1px solid var(--border-color)",
                  borderRadius: "8px",
                  padding: "10px 40px 10px 12px",
                  color: "#fff",
                }}
              />
              <button
                type="button"
                onClick={() => setShowCurrent(!showCurrent)}
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
                  display: "flex",
                }}
              >
                {showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {/* New Password + Auto-generate action */}
          <div style={{ marginBottom: "14px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "6px" }}>
              <label style={{ fontSize: "13px", color: "var(--text-muted)", margin: 0 }}>
                New Password
              </label>
              <button
                type="button"
                onClick={handleAutoGenerate}
                style={{
                  background: "rgba(10, 132, 255, 0.15)",
                  border: "1px solid rgba(10, 132, 255, 0.3)",
                  color: "#38bdf8",
                  padding: "3px 10px",
                  borderRadius: "6px",
                  fontSize: "12px",
                  fontWeight: 600,
                  cursor: "pointer",
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "5px",
                  transition: "all 0.2s",
                }}
                title="Automatically generate a secure password"
              >
                <Sparkles size={13} />
                Generate Strong
              </button>
            </div>
            <div style={{ position: "relative" }}>
              <input
                required
                type={showNew ? "text" : "password"}
                value={newPassword}
                placeholder="Enter new strong password"
                onChange={(e) => setNewPassword(e.target.value)}
                style={{
                  width: "100%",
                  paddingRight: "40px",
                  background: "rgba(255, 255, 255, 0.04)",
                  border: `1px solid ${strength.isValid ? "rgba(16, 185, 129, 0.5)" : "var(--border-color)"}`,
                  borderRadius: "8px",
                  padding: "10px 40px 10px 12px",
                  color: "#fff",
                }}
              />
              <button
                type="button"
                onClick={() => setShowNew(!showNew)}
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
                  display: "flex",
                }}
              >
                {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {/* Strong Password Matrix */}
          <PasswordStrengthMatrix password={newPassword} showDetails={true} />

          {/* Confirm New Password */}
          <div style={{ marginBottom: "20px" }}>
            <label style={{ display: "block", fontSize: "13px", color: "var(--text-muted)", marginBottom: "6px" }}>
              Confirm New Password
            </label>
            <div style={{ position: "relative" }}>
              <input
                required
                type={showConfirm ? "text" : "password"}
                value={confirmPassword}
                placeholder="Re-enter new password"
                onChange={(e) => setConfirmPassword(e.target.value)}
                style={{
                  width: "100%",
                  paddingRight: "40px",
                  background: "rgba(255, 255, 255, 0.04)",
                  border: `1px solid ${passwordsMatch ? "rgba(16, 185, 129, 0.5)" : "var(--border-color)"}`,
                  borderRadius: "8px",
                  padding: "10px 40px 10px 12px",
                  color: "#fff",
                }}
              />
              <button
                type="button"
                onClick={() => setShowConfirm(!showConfirm)}
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
                  display: "flex",
                }}
              >
                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {newPassword && confirmPassword && !passwordsMatch && (
              <span style={{ fontSize: "12px", color: "#f87171", marginTop: "4px", display: "block" }}>
                Passwords do not match
              </span>
            )}
          </div>

          <div className="form-actions" style={{ display: "flex", gap: "10px", marginTop: "16px" }}>
            <button
              type="submit"
              disabled={loading || !strength.isValid || !passwordsMatch}
              style={{
                flex: 1,
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "8px",
                opacity: loading || !strength.isValid || !passwordsMatch ? 0.6 : 1,
                cursor: loading || !strength.isValid || !passwordsMatch ? "not-allowed" : "pointer",
              }}
            >
              {loading && <Loader2 size={16} className="spin" />}
              {loading ? "Updating..." : "Update Password"}
            </button>
            <button
              type="button"
              onClick={handleClose}
              disabled={loading}
              style={{
                background: "rgba(255, 255, 255, 0.08)",
                color: "#e2e8f0",
                border: "1px solid rgba(255, 255, 255, 0.15)",
                flex: "0 0 100px",
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
