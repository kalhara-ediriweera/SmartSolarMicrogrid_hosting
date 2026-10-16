import { useEffect, useState } from "react";
import { CalendarDays, ShieldCheck, Activity, Battery } from "lucide-react";
import Title from "../../components/common/PageTitle";
import StatCard from "../../components/dashboard/StatCard";
import BranchNotice from "../../components/operator/BranchNotice";
import * as operatorService from "../../services/operatorService";

export default function OpHome() {
  const [d, setD] = useState({});
  const [me, setMe] = useState(null);

  useEffect(() => {
    operatorService.getDashboard()
      .then((x) => setD(x.data || {}))
      .catch(() => {});

    operatorService.getMe()
      .then((x) => setMe(x.data))
      .catch(() => {});
  }, []);

  return (
    <>
      <Title t="Grid Operator Dashboard" d="Operational station monitoring" />
      <BranchNotice me={me} />

      <div className="stats">
        <StatCard t="Pending" v={d.pendingReservations || 0} I={CalendarDays} />
        <StatCard t="Approved" v={d.approvedReservations || 0} I={ShieldCheck} />
        <StatCard t="Today's Bookings" v={d.todayBookings || 0} I={Activity} />
        <StatCard t="Available Slots" v={d.availableSlots || 0} I={Battery} />
      </div>
    </>
  );
}
