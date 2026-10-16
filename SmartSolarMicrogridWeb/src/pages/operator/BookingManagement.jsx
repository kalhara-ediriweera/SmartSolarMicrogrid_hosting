import { useEffect, useState } from "react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import BranchNotice from "../../components/operator/BranchNotice";
import * as operatorService from "../../services/operatorService";

const fmtKwh = (v) => {
  if (v == null || isNaN(v)) return "0";
  const n = Number(v);
  return Number.isInteger(n) ? n.toString() : Number(n.toFixed(3)).toString();
};

export default function Bookings() {
  const [st, setSt] = useState("Pending");
  const [a, setA] = useState([]);
  const [me, setMe] = useState(null);
  const [err, setErr] = useState("");

  const load = () => {
    operatorService.getReservations(st)
      .then((x) => setA(x.data || []))
      .catch((x) => setErr(x.response?.data?.error || "Failed to load reservations."));
  };

  useEffect(() => {
    operatorService.getMe()
      .then((x) => setMe(x.data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    load();
  }, [st]);

  async function approve(id) {
    try {
      await operatorService.approveReservation(id);
      setErr("");
      load();
    } catch (x) {
      setErr(x.response?.data?.error || "Failed to approve reservation.");
    }
  }

  return (
    <>
      <Title t="Bookings" d="Monitor and approve reservation workflow" />
      <BranchNotice me={me} />

      {err && <div className="err">{err}</div>}

      <Table>
        <div className="tabs">
          {["Pending", "Approved", "Completed", "Cancelled"].map((x) => (
            <button
              key={x}
              className={st === x ? "selected" : ""}
              onClick={() => setSt(x)}
            >
              {x}
            </button>
          ))}
        </div>

        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Prosumer</th>
              <th>Date</th>
              <th>Time</th>
              <th>Energy</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {a.length > 0 ? (
              a.map((r) => (
                <tr key={r.id}>
                  <td>{r.reservationCode}</td>
                  <td>{r.prosumerName || r.prosumerId}</td>
                  <td>{new Date(r.reservationDate).toLocaleDateString()}</td>
                  <td>{r.startTime}–{r.endTime}</td>
                  <td>{fmtKwh(r.energyAmountKwh)} kWh</td>
                  <td>
                    <span className="badge">{r.status}</span>
                  </td>
                  <td>
                    {st === "Pending" && (
                      <button
                        className="btn-success"
                        onClick={() => approve(r.id)}
                      >
                        Approve
                      </button>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7" style={{ textAlign: "center", padding: "24px", color: "#94a3b8" }}>
                  No {st.toLowerCase()} reservations for your branch.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Table>
    </>
  );
}
