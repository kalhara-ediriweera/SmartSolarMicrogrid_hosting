import { useEffect, useState } from "react";
import { AlertTriangle, X } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import BranchNotice from "../../components/operator/BranchNotice";
import * as operatorService from "../../services/operatorService";

const suggestions = ["Routine Maintenance", "Battery Fault", "Grid Repair", "Low Solar Output"];
const fmtKwh = (v) => {
    if (v == null || isNaN(v)) return "0";
    const n = Number(v);
    return Number.isInteger(n) ? n.toString() : Number(n.toFixed(3)).toString();
};

export default function Avail() {
    const [me, setMe] = useState(null), [a, setA] = useState([]), [reqs, setReqs] = useState([]);
    const [modal, setModal] = useState(null), [reason, setReason] = useState(""), [err, setErr] = useState("");

    const load = () => Promise.all([operatorService.getMe(), operatorService.getSlots(), operatorService.getMySlotRequests()])
        .then(([m, s, r]) => { setMe(m.data); setA(s.data); setReqs(r.data); });
    useEffect(() => { load(); }, []);

    function open(s) { setModal(s); setReason(""); setErr(""); }
    async function send(e) {
        e.preventDefault();
        try {
            await operatorService.requestSlotUnavailable(modal.id, reason);
            setModal(null); load();
        } catch (x) { setErr(x.response?.data?.error || "Failed to send request."); }
    }

    return <>
        <Title t="Energy Availability" d="Monitor your branch's energy slots and request unavailability" />
        <BranchNotice me={me} />
        <Table>
            <table>
                <thead><tr><th>Date</th><th>Slot</th><th>Total</th><th>Available</th><th>Status</th><th>Reason</th><th /></tr></thead>
                <tbody>
                    {a.length > 0 ? a.map(s => (
                        <tr key={s.id}>
                            <td>{new Date(s.slotDate).toLocaleDateString()}</td>
                            <td>{s.startTime}–{s.endTime}</td>
                            <td>{fmtKwh(s.energyAmountKwh)} kWh</td>
                            <td>{fmtKwh(s.availableCapacityKwh)} kWh</td>
                            <td><span className={`badge ${s.status === "Available" ? "badge-active" : "badge-inactive"}`}>{s.status}</span></td>
                            <td>{s.status === "Unavailable"
                                ? <span className="reason-pill" title={s.unavailableReason || "No reason provided"}><AlertTriangle size={12} />{s.unavailableReason || "No reason provided"}</span>
                                : "—"}</td>
                            <td style={{ textAlign: "right" }}>
                                {s.status === "Available" && (s.hasPendingRequest
                                    ? <span className="badge">Request Pending</span>
                                    : <button className="btn-delete" onClick={() => open(s)}>Request Unavailable</button>)}
                            </td>
                        </tr>
                    )) : <tr><td colSpan="7" style={{ textAlign: "center", padding: "24px", color: "#94a3b8" }}>No energy slots found for your branch.</td></tr>}
                </tbody>
            </table>
        </Table>

        {reqs.length > 0 && <Table>
            <h2 style={{ marginTop: 0 }}>My Unavailability Requests</h2>
            <table>
                <thead><tr><th>Date</th><th>Slot</th><th>Reason</th><th>Status</th><th>Admin Note</th></tr></thead>
                <tbody>{reqs.map(r => (
                    <tr key={r.id}>
                        <td>{r.slotDate ? new Date(r.slotDate).toLocaleDateString() : "—"}</td>
                        <td>{r.startTime ? `${r.startTime}–${r.endTime}` : "—"}</td>
                        <td>{r.reason}</td>
                        <td><span className={`badge ${r.status === "Rejected" ? "badge-inactive" : r.status === "Approved" ? "badge-active" : ""}`}>{r.status}</span></td>
                        <td>{r.adminNote || "—"}</td>
                    </tr>
                ))}</tbody>
            </table>
        </Table>}

        {modal && (
            <div className="modal-overlay" onClick={() => setModal(null)}>
                <div className="modal-card" onClick={e => e.stopPropagation()}>
                    <div className="modal-header">
                        <h2><AlertTriangle size={22} />Request Slot Unavailability</h2>
                        <button className="modal-close-btn" onClick={() => setModal(null)}><X size={20} /></button>
                    </div>
                    <div className="modal-info-box">
                        Slot <strong>{new Date(modal.slotDate).toLocaleDateString()} {modal.startTime}–{modal.endTime}</strong> will be marked <u>Unavailable</u> once the Backoffice admin approves this request.
                    </div>
                    {err && <div className="err" style={{ margin: "0 0 14px" }}>{err}</div>}
                    <form onSubmit={send}>
                        <span className="suggestion-chips-label">Quick Suggestions (Click to set reason):</span>
                        <div className="suggestion-chips">
                            {suggestions.map(s => <button key={s} type="button" className={`suggestion-chip ${reason === s ? "active" : ""}`} onClick={() => setReason(s)}>{s}</button>)}
                        </div>
                        <label style={{ display: "block", marginTop: 0 }}>
                            <span style={{ fontWeight: 600, color: "#f8fafc" }}>Reason * (Required):</span>
                            <textarea required rows={3} style={{ width: "100%", marginTop: "8px", resize: "vertical" }} placeholder="Why should this slot be unavailable?" value={reason} onChange={e => setReason(e.target.value)} />
                        </label>
                        <div className="form-actions" style={{ marginTop: "20px" }}>
                            <button type="submit" className="btn-delete" style={{ flex: 1, padding: "12px", fontSize: "14px", borderRadius: "12px" }}>Send Request</button>
                            <button type="button" onClick={() => setModal(null)} style={{ background: "#475569", flex: 1, padding: "12px", fontSize: "14px", borderRadius: "12px" }}>Cancel</button>
                        </div>
                    </form>
                </div>
            </div>
        )}
    </>;
}
