import { useEffect, useState } from "react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as bookingService from "../../services/bookingService";

const fmtKwh = (v) => {
  if (v == null || isNaN(v)) return "0";
  const n = Number(v);
  return Number.isInteger(n) ? n.toString() : Number(n.toFixed(3)).toString();
};

export default function Res() {
  const [st, setSt] = useState("Pending");
  const [a, setA] = useState([]);

  const load = () => {
    bookingService.getReservations(st)
      .then((x) => setA(x.data || []))
      .catch(() => setA([]));
  };

  useEffect(() => {
    load();
  }, [st]);

  const handleApprove = async (id) => {
    try {
      await bookingService.approveReservation(id);
      load();
    } catch (err) {
      console.error("Failed to approve reservation", err);
    }
  };

  return (
    <>
      <Title t="Reservations" d="Monitor and approve reservation workflow" />

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
                        onClick={() => handleApprove(r.id)}
                      >
                        Approve
                      </button>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6" style={{ textAlign: "center", padding: "24px", color: "#94a3b8" }}>
                  No {st.toLowerCase()} reservations found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Table>
    </>
  );
}
