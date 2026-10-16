import { Check, X, ShieldAlert, ShieldCheck } from "lucide-react";
import { evaluatePasswordStrength } from "../../utils/passwordUtils";

export default function PasswordStrengthMatrix({ password = "", showDetails = true }) {
  if (!password && !showDetails) return null;

  const strength = evaluatePasswordStrength(password);

  return (
    <div
      style={{
        marginTop: "10px",
        marginBottom: "14px",
        padding: "12px 14px",
        background: "rgba(15, 23, 42, 0.7)",
        border: `1px solid ${strength.percentage === 100 ? "rgba(16, 185, 129, 0.3)" : "rgba(255, 255, 255, 0.08)"}`,
        borderRadius: "10px",
        transition: "all 0.25s ease",
      }}
    >
      {/* Header with Strength Label and Percentage */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "8px",
        }}
      >
        <span
          style={{
            fontSize: "12px",
            fontWeight: 600,
            color: "#94a3b8",
            display: "flex",
            alignItems: "center",
            gap: "6px",
          }}
        >
          {strength.isValid ? (
            <ShieldCheck size={14} color="#10b981" />
          ) : (
            <ShieldAlert size={14} color={strength.color} />
          )}
          Password Strength:
          <span
            style={{
              color: strength.color,
              fontWeight: 700,
              textTransform: "uppercase",
              fontSize: "11px",
              padding: "2px 6px",
              background: `${strength.color}22`,
              borderRadius: "4px",
            }}
          >
            {strength.label}
          </span>
        </span>
        <span style={{ fontSize: "11px", color: "#64748b", fontWeight: 600 }}>
          {strength.fulfilledCount}/5 Requirements
        </span>
      </div>

      {/* Progress Bar */}
      <div
        style={{
          width: "100%",
          height: "6px",
          background: "rgba(255, 255, 255, 0.08)",
          borderRadius: "999px",
          overflow: "hidden",
          marginBottom: showDetails ? "12px" : "4px",
        }}
      >
        <div
          style={{
            width: `${Math.max(strength.percentage, password ? 15 : 0)}%`,
            height: "100%",
            background: strength.color,
            borderRadius: "999px",
            transition: "width 0.3s ease, background-color 0.3s ease",
            boxShadow: `0 0 8px ${strength.color}66`,
          }}
        />
      </div>

      {/* Criteria Matrix */}
      {showDetails && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))",
            gap: "6px 12px",
            marginTop: "6px",
          }}
        >
          {strength.criteria.map((item) => (
            <div
              key={item.id}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "7px",
                fontSize: "11.5px",
                color: item.valid ? "#10b981" : "#94a3b8",
                fontWeight: item.valid ? 600 : 400,
                transition: "color 0.2s ease",
              }}
            >
              <span
                style={{
                  width: "15px",
                  height: "15px",
                  borderRadius: "50%",
                  display: "inline-flex",
                  alignItems: "center",
                  justifyContent: "center",
                  background: item.valid ? "rgba(16, 185, 129, 0.2)" : "rgba(148, 163, 184, 0.1)",
                  border: `1px solid ${item.valid ? "#10b981" : "rgba(148, 163, 184, 0.3)"}`,
                  flexShrink: 0,
                }}
              >
                {item.valid ? (
                  <Check size={10} color="#10b981" strokeWidth={3} />
                ) : (
                  <X size={9} color="#64748b" strokeWidth={2.5} />
                )}
              </span>
              <span>{item.label}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
