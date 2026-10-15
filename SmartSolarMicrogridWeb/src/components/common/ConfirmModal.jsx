import { X } from "lucide-react";

export default function ConfirmModal({ title, children, confirmLabel = "Confirm", onConfirm, onCancel, busy = false }) {
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="confirm-modal-title" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <h2 id="confirm-modal-title">{title}</h2>
          <button className="modal-close-btn" type="button" onClick={onCancel} aria-label="Close">
            <X size={20} />
          </button>
        </div>
        <div>{children}</div>
        <div className="form-actions">
          <button type="button" onClick={onConfirm} disabled={busy}>{busy ? "Working..." : confirmLabel}</button>
          <button type="button" onClick={onCancel} disabled={busy}>Cancel</button>
        </div>
      </section>
    </div>
  );
}