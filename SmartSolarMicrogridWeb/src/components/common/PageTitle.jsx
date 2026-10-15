export default function PageTitle({ t, d }) {
  return (
    <div className="title">
      <h1>{t}</h1>
      <p>{d}</p>
    </div>
  );
}
