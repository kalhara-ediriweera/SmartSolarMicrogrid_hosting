export default function Loading({ label = "Loading..." }) {
  return <div className="loading-state" role="status" aria-live="polite">{label}</div>;
}