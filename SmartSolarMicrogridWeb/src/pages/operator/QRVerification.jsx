import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as bookingService from "../../services/bookingService";

export default function QR(){const[t,setT]=useState(""),[r,setR]=useState(null),[e,setE]=useState("");async function verify(){try{const x=await bookingService.verifyQrTransaction(t);setR(x.data);setE("")}catch(x){setE(x.response?.data?.error||"Invalid QR")}}async function complete(){try{const x=await bookingService.completeQrTransaction(t);setR(x.data)}catch(x){setE(x.response?.data?.error||"Cannot complete")}}return <><Title t="QR Transaction Verification" d="Verify and finalize Prosumer energy transfers"/><div className="card qr"><QrCode size={48}/><textarea placeholder="Paste scanned QR token" value={t} onChange={e=>setT(e.target.value)}/>{e&&<div className="err">{e}</div>}<div className="qr-actions"><button onClick={verify}>Verify Transaction</button>{r?.valid&&<button onClick={complete}>Complete Transfer</button>}</div>{r&&<pre>{JSON.stringify(r,null,2)}</pre>}</div></>}
