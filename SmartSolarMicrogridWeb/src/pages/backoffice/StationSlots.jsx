import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as stationService from "../../services/stationService";
import * as bookingService from "../../services/bookingService";
import SlotRequestsPanel from "../../components/backoffice/SlotRequestsPanel";

export default function Slots(){
    const empty = {nodeId:"",slotDate:"",startTime:"08:00",endTime:"09:00",energyAmountKwh:"",availableCapacityKwh:"",status:"Available"};
    const [f,setF]=useState(empty),[nodes,setN]=useState([]),[a,setA]=useState([]),[edit,setEdit]=useState(null),[originalSlot,setOriginalSlot]=useState(null),[err,setErr]=useState("");
    const [search, setSearch] = useState("");

    const load = () => Promise.all([stationService.getStations(), bookingService.getEnergySlots()])
        .then(([n, s]) => {
            setN(n.data || []);
            setA(s.data || []);
        })
        .catch((x) => {
            setErr(x.response?.data?.error || "Failed to load energy slots and stations data.");
        });
    useEffect(() => { load(); }, []);

    const selectedNode = nodes.find(n => n.id === f.nodeId);
    const isSelectedNodeInactive = selectedNode && selectedNode.status !== 0 && selectedNode.status !== "Active";
    const reservedAmount = (edit && originalSlot) ? Math.max(0, (originalSlot.energyAmountKwh || 0) - (originalSlot.availableCapacityKwh || 0)) : 0;
    const today = new Date().toISOString().slice(0, 10);
    const currentTime = new Date().toISOString().slice(11, 16);
    const isFutureSlotStart = (date, time) => new Date(`${date}T${time}:00Z`) > new Date();
    const getSlotDurationHours = (start, end) => {
        const [startHours, startMinutes] = start.split(":").map(Number);
        const [endHours, endMinutes] = end.split(":").map(Number);
        const durationMinutes = endHours * 60 + endMinutes - startHours * 60 - startMinutes;
        return Number.isFinite(durationMinutes) && durationMinutes > 0 ? durationMinutes / 60 : null;
    };
    const slotDurationHours = getSlotDurationHours(f.startTime, f.endTime);
    const maxEnergyKwh = selectedNode && slotDurationHours
        ? selectedNode.capacityKw * slotDurationHours
        : null;

    async function go(e){
        e.preventDefault();
        setErr("");
        if (!slotDurationHours) {
            setErr("Slot end time must be after its start time.");
            return;
        }
        if (!isFutureSlotStart(f.slotDate, f.startTime)) {
            setErr("Energy slots must start in the future.");
            return;
        }
        if (maxEnergyKwh !== null && +f.energyAmountKwh > maxEnergyKwh + 1e-9) {
            setErr(`Total energy cannot exceed ${maxEnergyKwh.toFixed(2)} kWh for this node and slot duration.`);
            return;
        }
        if (edit && isSelectedNodeInactive && f.status === "Available") {
            setErr(`Cannot set slot status to Available because microgrid node '${selectedNode.nodeName}' is deactivated (${selectedNode.adminNote || "Inactive"}).`);
            return;
        }

        // Duplicate check when creating a new slot on same node, date, and time range
        if (!edit) {
            const formattedDate = new Date(f.slotDate).toISOString().split('T')[0];
            const duplicate = a.find(s => 
                s.nodeId === f.nodeId &&
                new Date(s.slotDate).toISOString().split('T')[0] === formattedDate &&
                s.startTime.trim() === f.startTime.trim() &&
                s.endTime.trim() === f.endTime.trim()
            );

            if (duplicate) {
                const nodeName = selectedNode ? selectedNode.nodeName : "this station";
                setErr(`An energy slot already exists for station '${nodeName}' on ${formattedDate} (${f.startTime} - ${f.endTime}). Please edit the existing slot to update its energy amount instead of creating a duplicate.`);
                return;
            }
        }

        // Check that total energy cannot be reduced below already reserved energy
        if (edit && originalSlot) {
            if (+f.energyAmountKwh < reservedAmount) {
                setErr(`Cannot reduce total energy (${f.energyAmountKwh} kWh) below already reserved energy (${reservedAmount.toFixed(1)} kWh).`);
                return;
            }
        }

        try {
            const b = {...f, energyAmountKwh: +f.energyAmountKwh, slotDate: new Date(f.slotDate).toISOString()};
            if (edit) {
                b.availableCapacityKwh = +f.availableCapacityKwh;
                b.status = f.status.toString();
                await bookingService.updateEnergySlot(edit, b);
            } else {
                await bookingService.createEnergySlot(b);
            }
            setEdit(null); setOriginalSlot(null); setF(empty); setErr(""); load();
        } catch(x) { 
            const errorMsg = x.response?.data?.error || x.response?.data?.message || (typeof x.response?.data === 'string' ? x.response.data : "Failed to save slot. Check inputs.");
            setErr(errorMsg); 
        }
    }

    function startEdit(s) {
        const statuses = ["Available", "Reserved", "Unavailable", "Completed"];
        setEdit(s.id);
        setOriginalSlot(s);
        const nodeForSlot = nodes.find(n => n.id === s.nodeId);
        const rawStatus = typeof s.status === 'number' ? statuses[s.status] : s.status;
        setF({
            nodeId: s.nodeId,
            slotDate: new Date(s.slotDate).toISOString().split('T')[0],
            startTime: s.startTime,
            endTime: s.endTime,
            energyAmountKwh: s.energyAmountKwh,
            availableCapacityKwh: s.availableCapacityKwh,
            status: (nodeForSlot && nodeForSlot.status !== 0 && nodeForSlot.status !== "Active" && rawStatus === "Available") ? "Unavailable" : rawStatus
        });
        setErr("");
    }

    function handleTotalEnergyChange(newTotal) {
        if (edit && originalSlot) {
            const reserved = Math.max(0, (originalSlot.energyAmountKwh || 0) - (originalSlot.availableCapacityKwh || 0));
            const autoAvailable = Math.max(0, (+newTotal || 0) - reserved);
            setF({
                ...f,
                energyAmountKwh: newTotal,
                availableCapacityKwh: Number(autoAvailable.toFixed(2))
            });
        } else {
            setF({
                ...f,
                energyAmountKwh: newTotal,
                availableCapacityKwh: newTotal
            });
        }
    }

    const filteredSlots = a.filter(s => {
        if (!search.trim()) return true;
        const q = search.toLowerCase();
        const node = nodes.find(n => n.id === s.nodeId);
        return (
            node?.nodeCode?.toLowerCase().includes(q) ||
            node?.nodeName?.toLowerCase().includes(q) ||
            node?.adminNote?.toLowerCase().includes(q) ||
            s.startTime?.includes(q) ||
            s.endTime?.includes(q) ||
            new Date(s.slotDate).toLocaleDateString().includes(q)
        );
    });
    return <>
        <Title t="Energy Slots" d="Create and monitor trading slots"/>
        <SlotRequestsPanel onResolved={load}/>
        <div className="grid2">
            <form className="card" onSubmit={go}>
                <h2>{edit ? "Edit Energy Slot" : "Create Energy Slot"}</h2>
                {err && <div className="err">{err}</div>}

                {isSelectedNodeInactive && (
                    <div className="err" style={{ background: "rgba(245, 158, 11, 0.12)", borderColor: "rgba(245, 158, 11, 0.35)", color: "#fef08a" }}>
                        <AlertTriangle size={18} style={{ flexShrink: 0 }}/>
                        <div>
                            <strong>Branch Deactivated:</strong> {selectedNode.nodeCode} ({selectedNode.nodeName}) is currently inactive.
                            <div style={{ fontSize: "12px", marginTop: "3px" }}>
                                Reason: <em>"{selectedNode.adminNote || selectedNode.deactivationReason || "Routine Maintenance"}"</em>. Slots cannot be made Available.
                            </div>
                        </div>
                    </div>
                )}

                <label>Node
                    <select required disabled={!!edit} value={f.nodeId} onChange={e=>setF({...f,nodeId:e.target.value})}>
                        <option value="">Select Microgrid Node</option>
                        {nodes.map(n=>{
                            const isInactive = n.status !== 0 && n.status !== "Active";
                            return (
                                <option value={n.id} key={n.id} disabled={isInactive}>
                                    {n.nodeCode} — {n.nodeName} {isInactive ? `⚠️ [Deactivated: ${n.adminNote || "Unavailable"}]` : ""}
                                </option>
                            );
                        })}
                    </select>
                </label>
                <label>Date<input required type="date" min={today} value={f.slotDate} onChange={e=>setF({...f,slotDate:e.target.value})}/></label>
                <label>Start<input type="time" required min={f.slotDate === today ? currentTime : undefined} value={f.startTime} onChange={e=>setF({...f,startTime:e.target.value})}/></label>
                <label>End<input type="time" required min={f.startTime} value={f.endTime} onChange={e=>setF({...f,endTime:e.target.value})}/></label>
                <label>Total Energy (kWh)
                    <input
                        required
                        type="number"
                        min="0.1"
                        step="0.1"
                        max={maxEnergyKwh ?? undefined}
                        value={f.energyAmountKwh}
                        onChange={e=>handleTotalEnergyChange(e.target.value)}
                    />
                    {maxEnergyKwh !== null && (
                        <span style={{ fontSize: "11px", color: "#94a3b8", marginTop: "4px", display: "block" }}>
                            Maximum for this slot: {maxEnergyKwh.toFixed(2)} kWh ({selectedNode.capacityKw} kW × {slotDurationHours} h).
                        </span>
                    )}
                </label>
                {edit && (
                    <>
                        <label>Available (kWh)
                            <input
                                readOnly
                                disabled
                                type="number"
                                value={f.availableCapacityKwh}
                                style={{
                                    backgroundColor: "rgba(148, 163, 184, 0.1)",
                                    cursor: "not-allowed",
                                    color: "#94a3b8",
                                    borderColor: "rgba(148, 163, 184, 0.25)"
                                }}
                            />
                            <span style={{ fontSize: "11px", color: "#94a3b8", marginTop: "4px", display: "block" }}>
                                🔒 Automatically managed based on reservations ({reservedAmount > 0 ? `${reservedAmount.toFixed(1)} kWh currently reserved` : "No reservations yet"}).
                            </span>
                        </label>
                        <label>Status
                            <select
                                value={f.status}
                                disabled={isSelectedNodeInactive}
                                onChange={e=>setF({...f,status:e.target.value})}
                            >
                                <option value="Available" disabled={isSelectedNodeInactive}>
                                    Available {isSelectedNodeInactive ? "(Locked - Node Inactive)" : ""}
                                </option>
                                <option value="Unavailable">Unavailable</option>
                            </select>
                        </label>
                    </>
                )}
                <div className="form-actions">
                    <button type="submit" style={{flex:1}}>{edit ? "Update Slot" : "Create Slot"}</button>
                    {edit && <button type="button" onClick={() => { setEdit(null); setOriginalSlot(null); setF(empty); setErr(""); }} style={{background:"#94a3b8",flex:1}}>Cancel</button>}
                </div>
            </form>

            <Table>
                <div className="table-header">
                    <h2 style={{ margin: 0 }}>Energy Slots</h2>
                    <div className="table-search-bar">
                        <Search size={16}/>
                        <input
                            type="text"
                            placeholder="Filter by node, date..."
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                        {search && (
                            <button type="button" onClick={() => setSearch("")} style={{ background: "transparent", border: "none", color: "#94a3b8", cursor: "pointer", padding: "0 4px", display: "flex", alignItems: "center" }}>
                                <X size={14}/>
                            </button>
                        )}
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Node / Branch</th>
                            <th>Time</th>
                            <th>Total</th>
                            <th>Available</th>
                            <th>Status</th>
                            <th style={{ textAlign: "right", paddingRight: "18px" }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredSlots.length > 0 ? filteredSlots.map(s => {
                            const node = nodes.find(n => n.id === s.nodeId);
                            const isNodeInactive = node && node.status !== 0 && node.status !== "Active";
                            const slotDate = new Date(s.slotDate).toISOString().slice(0, 10);
                            const isPastSlot = !isFutureSlotStart(slotDate, s.startTime);
                            const statusStr = typeof s.status === 'number' ? ["Available", "Reserved", "Unavailable", "Completed"][s.status] : s.status;
                            return (
                                <tr key={s.id}>
                                    <td>{new Date(s.slotDate).toLocaleDateString()}</td>
                                    <td>
                                        <div className="cell-branch-stack">
                                            <div className="branch-title-row">
                                                <strong>{node?.nodeCode || s.nodeId}</strong>
                                                {node?.nodeName && <span className="branch-subname">({node.nodeName})</span>}
                                            </div>
                                            {isNodeInactive && (
                                                <span className="branch-inactive-tag" title={`Reason: ${node.adminNote || "Routine Maintenance"}`}>
                                                    <AlertTriangle size={11} style={{ flexShrink: 0 }}/>
                                                    <span>{node.adminNote || "Node Deactivated"}</span>
                                                </span>
                                            )}
                                        </div>
                                    </td>
                                    <td>{s.startTime}–{s.endTime}</td>
                                    <td>{s.energyAmountKwh} kWh</td>
                                    <td>{s.availableCapacityKwh} kWh</td>
                                    <td>
                                        <span className={`badge ${statusStr === "Available" ? "badge-active" : "badge-inactive"}`}>
                                            {statusStr}
                                        </span>
                                    </td>
                                    <td style={{ textAlign: "right", paddingRight: "18px" }}>
                                        <button className="btn-edit" disabled={isPastSlot} title={isPastSlot ? "Past slots cannot be edited" : undefined} onClick={()=>startEdit(s)}>Edit</button>
                                    </td>
                                </tr>
                            );
                        }) : (
                            <tr>
                                <td colSpan="7" style={{ textAlign: "center", padding: "24px", color: "#94a3b8" }}>
                                    No energy slots found.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </Table>
        </div>
    </>;
}
