export default function ErrorMessage({ children, onDismiss }) {
  if (!children) return null;
  return (
    <div className="err" role="alert">
      <span>{children}</span>
      {onDismiss && <button type="button" onClick={onDismiss} aria-label="Dismiss error">Dismiss</button>}
    </div>
  );
}