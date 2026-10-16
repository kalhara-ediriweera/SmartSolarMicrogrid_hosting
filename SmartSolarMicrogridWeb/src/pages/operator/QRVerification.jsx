import { useState } from "react";
import { QrCode } from "lucide-react";
import Title from "../../components/common/PageTitle";
import * as bookingService from "../../services/bookingService";

export default function QR() {
  const [t, setT] = useState("");
  const [r, setR] = useState(null);
  const [e, setE] = useState("");

  async function verify() {
    try {
      const x = await bookingService.verifyQrTransaction(t);
      setR(x.data);
      setE("");
    } catch (x) {
      setE(x.response?.data?.error || "Invalid QR code token.");
    }
  }

  async function complete() {
    try {
      const x = await bookingService.completeQrTransaction(t);
      setR(x.data);
      setE("");
    } catch (x) {
      setE(x.response?.data?.error || "Cannot complete energy transfer.");
    }
  }

  return (
    <>
      <Title t="QR Transaction Verification" d="Verify and finalize Prosumer energy transfers" />

      <div className="card qr">
        <QrCode size={48} />
        <textarea
          rows={3}
          placeholder="Paste scanned QR token here..."
          value={t}
          onChange={(e) => setT(e.target.value)}
        />

        {e && <div className="err">{e}</div>}

        <div className="qr-actions">
          <button type="button" onClick={verify}>
            Verify Transaction
          </button>
          {r?.valid && (
            <button type="button" className="btn-success" onClick={complete}>
              Complete Transfer
            </button>
          )}
        </div>

        {r && (
          <pre style={{ marginTop: "16px", padding: "12px", background: "rgba(0,0,0,0.2)", borderRadius: "8px" }}>
            {JSON.stringify(r, null, 2)}
          </pre>
        )}
      </div>
    </>
  );
}
