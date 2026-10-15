import { useEffect, useState } from "react";
import {Routes,Route,useLocation} from "react-router-dom";
import AppRoutes from "./routes/AppRoutes";
import Login from "./pages/Login";
import Navbar from "./components/common/Navbar";
import Sidebar from "./components/common/Sidebar";
import BackofficeDashboard from "./pages/backoffice/BackofficeDashboard";
import UserManagement from "./pages/backoffice/UserManagement";
import ProsumerManagement from "./pages/backoffice/ProsumerManagement";
import StationManagement from "./pages/backoffice/StationManagement";
import BackofficeSlots from "./pages/backoffice/StationSlots";
import ReservationManagement from "./pages/backoffice/ReservationManagement";
import OperatorDashboard from "./pages/operator/OperatorDashboard";
import OperatorBookings from "./pages/operator/BookingManagement";
import OperatorAvailability from "./pages/operator/StationSlots";
import OperatorQR from "./pages/operator/QRVerification";

function Shell({role}){
    const l=useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(()=>{ setMobileOpen(false); }, [l.pathname]);

  const toggleSidebar = () => {
    if (window.innerWidth <= 1024) {
      setMobileOpen(prev => !prev);
    } else {
      setCollapsed(prev => !prev);
    }
  };

  return (
    <div className="app-shell">
            <Sidebar role={role} mobileOpen={mobileOpen} collapsed={collapsed} onClose={()=>setMobileOpen(false)}/>
      <main className={`main ${collapsed ? "collapsed" : ""}`}>
                <Navbar collapsed={collapsed} onToggle={toggleSidebar}/>
        <section>
          <Routes>
            {role==="Backoffice"?<><Route index element={<BackofficeDashboard/>}/><Route path="users" element={<UserManagement/>}/><Route path="prosumers" element={<ProsumerManagement/>}/><Route path="stations" element={<StationManagement/>}/><Route path="slots" element={<BackofficeSlots/>}/><Route path="reservations" element={<ReservationManagement/>}/></>:<><Route index element={<OperatorDashboard/>}/><Route path="bookings" element={<OperatorBookings/>}/><Route path="availability" element={<OperatorAvailability/>}/><Route path="qr" element={<OperatorQR/>}/></>}
          </Routes>
        </section>
      </main>
    </div>
  );
}


export default function App(){
    return <AppRoutes LoginPage={Login} BackofficePage={()=> <Shell role="Backoffice"/>} OperatorPage={()=> <Shell role="GridOperator"/>}/>;
}