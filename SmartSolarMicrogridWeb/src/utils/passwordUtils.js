/**
 * Password utility functions for generation and strength matrix evaluation.
 */

export function generateStrongPassword(length = 14) {
  const upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // exclude easily confused chars like I, O
  const lower = "abcdefghijkmnopqrstuvwxyz"; // exclude l
  const numbers = "23456789"; // exclude 0, 1
  const special = "!@#$%^&*()_+-=[]{}|;:,.<>?";

  // Guarantee at least one of each required category
  const guaranteed = [
    upper[Math.floor(Math.random() * upper.length)],
    lower[Math.floor(Math.random() * lower.length)],
    numbers[Math.floor(Math.random() * numbers.length)],
    special[Math.floor(Math.random() * special.length)],
  ];

  const allChars = upper + lower + numbers + special;
  const remaining = [];
  for (let i = guaranteed.length; i < Math.max(length, 12); i++) {
    remaining.push(allChars[Math.floor(Math.random() * allChars.length)]);
  }

  // Combine and shuffle using Fisher-Yates
  const combined = [...guaranteed, ...remaining];
  for (let i = combined.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [combined[i], combined[j]] = [combined[j], combined[i]];
  }

  return combined.join("");
}

export function evaluatePasswordStrength(password = "") {
  const p = password || "";
  
  const matrix = {
    hasMinLength: p.length >= 8,
    hasUpper: /[A-Z]/.test(p),
    hasLower: /[a-z]/.test(p),
    hasNumber: /[0-9]/.test(p),
    hasSpecial: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(p),
  };

  const criteria = [
    { id: "length", label: "At least 8 characters", valid: matrix.hasMinLength },
    { id: "upper", label: "At least 1 uppercase letter (A-Z)", valid: matrix.hasUpper },
    { id: "lower", label: "At least 1 lowercase letter (a-z)", valid: matrix.hasLower },
    { id: "number", label: "At least 1 numeric digit (0-9)", valid: matrix.hasNumber },
    { id: "special", label: "At least 1 special character (!@#$%...)", valid: matrix.hasSpecial },
  ];

  const fulfilledCount = criteria.filter((c) => c.valid).length;
  const percentage = Math.round((fulfilledCount / criteria.length) * 100);

  let label = "Very Weak";
  let color = "#ef4444"; // red

  if (p.length === 0) {
    label = "Empty";
    color = "#64748b";
  } else if (fulfilledCount === 5) {
    label = "Strong";
    color = "#10b981"; // green
  } else if (fulfilledCount >= 4) {
    label = "Good";
    color = "#38bdf8"; // sky blue
  } else if (fulfilledCount >= 3) {
    label = "Fair";
    color = "#f59e0b"; // amber
  } else if (fulfilledCount >= 2) {
    label = "Weak";
    color = "#f97316"; // orange
  }

  return {
    matrix,
    criteria,
    fulfilledCount,
    percentage,
    isValid: fulfilledCount === 5,
    label,
    color,
  };
}
