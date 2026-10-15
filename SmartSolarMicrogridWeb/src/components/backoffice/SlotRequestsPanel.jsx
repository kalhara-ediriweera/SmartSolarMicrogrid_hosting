import { useEffect, useState } from "react";
import Table from "../common/DataTable";
import * as operatorService from "../../services/operatorService";

// Pending "make slot unavailable" requests sent by grid operators.
export default function SlotRequestsPanel({ onResolved }) {
  const [a, setA] = useState([]), [err, setErr] = useState("");
  const load = () => operatorService.getSlotRequests("Pending").then(x => setA(x.data));
  useEffect(() => { load(); }, []);

  async function resolve(r, approve) {
    const note = approve ? "" : window.prompt("Reason for rejecting (optional)", "");
    if (note === null) return;
    try {
      await (approve ? operatorService.approveSlotRequest(r.id, note) : operatorService.rejectSlotRequest(r.id, note));
      setErr(""); load(); onResolved?.();
    } catch (x) { setErr(x.response?.data?.error || "Failed to update request."); }
  }

  if (a.length === 0 && !err) return null;
  return (
    <Table>
      <h2 style={{ marginTop: 0 }}>Slot Unavailability Requests</h2>
      {err && <div className="err">{err}</div>}
      <table>
        <thead><tr><th>Branch</th><th>Date</th><th>Slot</th><th>Requested By</th><th>Reason</th><th style={{ textAlign: "right", paddingRight: "18px" }}>Actions</th></tr></thead>
        <tbody>{a.map(r => (
          <tr key={r.id}>
            <td><strong>{r.nodeCode || r.nodeId}</strong> {r.nodeName && <span className="branch-subname">({r.nodeName})</span>}</td>
            <td>{r.slotDate ? new Date(r.slotDate).toLocaleDateString() : "—"}</td>
            <td>{r.startTime ? `${r.startTime}–${r.endTime}` : "—"}</td>
            <td>{r.requestedByUsername}</td>
            <td>{r.reason}</td>
            <td style={{ textAlign: "right", paddingRight: "18px" }}>
              <button className="btn-success" onClick={() => resolve(r, true)}>Approve</button>
              <button className="btn-delete" onClick={() => resolve(r, false)}>Reject</button>
            </td>
          </tr>
        ))}</tbody>
      </table>
    </Table>
  );
}
