import { useEffect, useState } from "react";
import Table from "../common/DataTable";
import * as operatorService from "../../services/operatorService";
import * as stationService from "../../services/stationService";

// Links each grid operator account to the branch (microgrid node) they manage.
export default function OperatorBranchAssignments() {
  const [a, setA] = useState([]), [nodes, setNodes] = useState([]), [err, setErr] = useState("");
  const load = () => Promise.all([operatorService.getOperatorAssignments(), stationService.getStations()])
    .then(([o, n]) => { setA(o.data); setNodes(n.data); });
  useEffect(() => { load(); }, []);

  async function assign(userId, stationId) {
    try { await operatorService.assignOperatorStation(userId, stationId || null); setErr(""); load(); }
    catch (x) { setErr(x.response?.data?.error || "Failed to assign branch."); }
  }

  return (
    <Table>
      <h2 style={{ marginTop: 0 }}>Grid Operator Branches</h2>
      {err && <div className="err">{err}</div>}
      <table>
        <thead><tr><th>Operator</th><th>Assigned Branch</th></tr></thead>
        <tbody>{a.length > 0 ? a.map(o => (
          <tr key={o.userId}>
            <td>{o.username}</td>
            <td>
              <select value={o.stationId || ""} onChange={e => assign(o.userId, e.target.value)} style={{ marginTop: 0, maxWidth: 320 }}>
                <option value="">— Not assigned —</option>
                {nodes.map(n => <option key={n.id} value={n.id}>{n.nodeCode} — {n.nodeName}</option>)}
              </select>
            </td>
          </tr>
        )) : <tr><td colSpan="2" style={{ textAlign: "center", padding: "20px", color: "#64748b" }}>No grid operators found.</td></tr>}</tbody>
      </table>
    </Table>
  );
}
