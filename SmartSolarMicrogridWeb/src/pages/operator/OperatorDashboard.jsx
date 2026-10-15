import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import StatCard from "../../components/dashboard/StatCard";
import BranchNotice from "../../components/operator/BranchNotice";
import * as operatorService from "../../services/operatorService";

export default function OpHome(){const[d,setD]=useState({}),[me,setMe]=useState(null);useEffect(()=>{operatorService.getDashboard().then(x=>setD(x.data));operatorService.getMe().then(x=>setMe(x.data))},[]);return <><Title t="Grid Operator Dashboard" d="Operational station monitoring"/><BranchNotice me={me}/><div className="stats"><StatCard t="Pending" v={d.pendingReservations} I={CalendarDays}/><StatCard t="Approved" v={d.approvedReservations} I={ShieldCheck}/><StatCard t="Today's Bookings" v={d.todayBookings} I={Activity}/><StatCard t="Available Slots" v={d.availableSlots} I={Battery}/></div></>}
