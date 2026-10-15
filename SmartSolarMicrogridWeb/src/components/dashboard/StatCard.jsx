export default function StatCard({ t, v, I: Icon }) {
  return (
    <div className="card stat">
      <div>
        <small>{t}</small>
        <strong>{v ?? "—"}</strong>
      </div>
      <Icon />
    </div>
  );
}