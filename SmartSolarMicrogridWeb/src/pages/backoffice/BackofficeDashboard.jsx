import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import StatCard from "../../components/dashboard/StatCard";
import { api } from "../../services/api";

export default function BOHome(){const[d,setD]=useState({});useEffect(()=>{api.get("/dashboard/backoffice").then(x=>setD(x.data))},[]);return <><Title t="Backoffice Dashboard" d="Administration and microgrid overview"/><div className="stats"><StatCard t="Active Prosumers" v={d.totalProsumers} I={Users}/><StatCard t="Pending Accounts" v={d.pendingProsumerAccounts} I={ShieldCheck}/><StatCard t="Microgrid Nodes" v={d.totalStations} I={MapPin}/><StatCard t="Available Slots" v={d.availableSlots} I={Battery}/><StatCard t="Pending Reservations" v={d.pendingReservations} I={CalendarDays}/><StatCard t="Approved Reservations" v={d.approvedReservations} I={Activity}/><StatCard t="Completed Transfers" v={d.completedTransfers} I={QrCode}/></div></>}
