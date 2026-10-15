import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as prosumerService from "../../services/prosumerService";

export default function Pros(){
    const [a, setA] = useState([]), [st, setSt] = useState("Pending");
    const [viewNic, setViewNic] = useState(null);
    const [solar, setSolar] = useState({});
    const [modalAction, setModalAction] = useState("");
    const [warningModal, setWarningModal] = useState(null);
    const load = () => prosumerService.getProsumers().then(x => setA(x.data)).catch(() => {});
    useEffect(() => { load(); }, []);
    const act = async (n, action) => { 
        await prosumerService.updateProsumerStatus(n, action);
        load(); 
        setViewNic(null); 
        if (action === "activate") setSt("Active");
        if (action === "reject") setSt("Rejected");
    };
    const filtered = a.filter(p => 
        st === "Pending" ? (p.accountStatus === 0 || p.accountStatus === "Pending") : 
        st === "Active" ? (p.accountStatus === 1 || p.accountStatus === "Active") : 
        st === "Rejected" ? (p.accountStatus === 3 || p.accountStatus === "Rejected") : 
        (p.accountStatus === 2 || p.accountStatus === "Deactivated")
    );

    const renderImg = (b64) => {
        if (!b64) return <p style={{color: '#94a3b8', fontStyle: 'italic', fontSize: '14px', margin: 0}}>Not provided</p>;
        const src = b64.startsWith('data:') ? b64 : `data:image/jpeg;base64,${b64}`;
        return <img src={src} alt="NIC" style={{width: '100%', borderRadius: '8px', border: '1px solid #334155'}}/>;
    };

    return <><Title t="Prosumer Management" d="Manage prosumer profiles and lifecycle" /><Table>
        <div className="table-header" style={{ marginBottom: '15px', alignItems: 'center' }}>
            <h2 style={{ margin: 0 }}>Prosumers List</h2>
            <div className="tabs" style={{ margin: 0 }}>
                <button className={st === "Pending" ? "selected" : ""} onClick={() => setSt("Pending")}>Pending</button>
                <button className={st === "Active" ? "selected" : ""} onClick={() => setSt("Active")}>Active</button>
                <button className={st === "Inactive" ? "selected" : ""} onClick={() => setSt("Inactive")}>Deactivated</button>
                <button className={st === "Rejected" ? "selected" : ""} onClick={() => setSt("Rejected")}>Rejected</button>
            </div>
        </div>
        <table>
            <thead><tr><th>NIC</th><th>Name</th><th>Email</th><th>Phone</th><th>Status</th><th>Documents</th><th>Actions</th></tr></thead>
            <tbody>{filtered.map(p => <tr key={p.id}>
                <td>{p.nic}</td><td>{p.fullName}</td><td>{p.email}</td><td>{p.phone}</td>
                <td><span className="badge">{p.accountStatus === 0 || p.accountStatus === "Pending" ? ((p.isDrpVerified || p.IsDrpVerified) ? "NIC Verified" : "Pending") : p.accountStatus === 1 || p.accountStatus === "Active" ? "Active" : "Inactive"}</span></td>
                <td>
                    <button className="btn-edit" onClick={async () => {
                        let updatedP = { ...p };
                        setViewNic(updatedP);
                        if (!updatedP.extractedNicNumber && updatedP.nicFrontImageBase64) {
                            try {
                                const src = updatedP.nicFrontImageBase64.startsWith('data:') ? updatedP.nicFrontImageBase64 : `data:image/jpeg;base64,${updatedP.nicFrontImageBase64}`;
                                const result = await window.Tesseract.recognize(src, 'eng');
                                
                                // Clean up the text by removing spaces and most punctuation, keep letters/numbers
                                const text = result.data.text.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
                                
                                // Try to find the exact NIC or any 9-12 digit sequence
                                const match = text.match(/(\d{9}[VX]|\d{12})/);
                                if (match) {
                                    updatedP.extractedNicNumber = match[0];
                                } else {
                                    // If strict match fails, try to see if the typed NIC is just hidden in the noisy text
                                    if (text.includes(updatedP.nic.toUpperCase())) {
                                        updatedP.extractedNicNumber = updatedP.nic.toUpperCase();
                                    } else {
                                        // Try to grab any large number sequence (8+ digits) as a best guess
                                        const fuzzyMatch = text.match(/\d{8,}/);
                                        updatedP.extractedNicNumber = fuzzyMatch ? fuzzyMatch[0] : "NOT_FOUND";
                                    }
                                }
                                setViewNic({...updatedP});
                            } catch (e) {
                                console.error(e);
                                updatedP.extractedNicNumber = "NOT_FOUND";
                                setViewNic({...updatedP});
                            }
                        }
                    }} style={{fontSize: '13px', padding: '6px 10px'}}>View NIC</button>
                </td>
                <td>
                    {st === "Pending" && (
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: 500, color: (p.isDrpVerified || p.IsDrpVerified) ? '#475569' : '#94a3b8', cursor: (p.isDrpVerified || p.IsDrpVerified) ? 'pointer' : 'not-allowed' }} title={(p.isDrpVerified || p.IsDrpVerified) ? "Verify Solar Panel Installation" : "Please Verify NIC First"}>
                            <input 
                                type="checkbox" 
                                disabled={!(p.isDrpVerified || p.IsDrpVerified)}
                                checked={!!solar[p.nic]} 
                                onChange={e => {
                                    const checked = e.target.checked;
                                    setSolar({...solar, [p.nic]: checked});
                                    if (checked && (p.isDrpVerified || p.IsDrpVerified)) {
                                        setWarningModal(p);
                                    }
                                }} 
                                style={{ cursor: (p.isDrpVerified || p.IsDrpVerified) ? 'pointer' : 'not-allowed', width: '15px', height: '15px', accentColor: '#10b981' }} 
                            /> 
                            Verify Solar
                        </label>
                    )}
                    {st === "Active" && <button className="btn-delete" onClick={() => act(p.nic, "deactivate")}>Deactivate</button>}
                    {st === "Inactive" && <button className="btn-success" onClick={() => act(p.nic, "reactivate")}>Reactivate</button>}
                </td>
            </tr>)}</tbody>
        </table>
        {!filtered.length && <p className="empty">No {st.toLowerCase()} prosumers found.</p>}
    </Table>
    {viewNic && (
        <div className="modal-overlay" onClick={() => setViewNic(null)}>
            <div className="modal-card" onClick={e => e.stopPropagation()} style={{maxWidth: "500px", maxHeight: '90vh', overflowY: 'auto'}}>
                <div className="modal-header">
                    <h2>NIC Documents: {viewNic.nic}</h2>
                    <button className="modal-close-btn" onClick={() => setViewNic(null)}><X size={20}/></button>
                </div>
                
                {viewNic.extractedNicNumber && viewNic.extractedNicNumber !== "NOT_FOUND" ? (
                    <div style={{ padding: '12px', background: viewNic.nic.toUpperCase() === viewNic.extractedNicNumber.toUpperCase() ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)', border: `1px solid ${viewNic.nic.toUpperCase() === viewNic.extractedNicNumber.toUpperCase() ? '#10b981' : '#ef4444'}`, borderRadius: '8px', margin: '15px 0 0 0' }}>
                        <div style={{ fontWeight: 'bold', color: viewNic.nic.toUpperCase() === viewNic.extractedNicNumber.toUpperCase() ? '#10b981' : '#ef4444', marginBottom: '5px', display: 'flex', alignItems: 'center', gap: '5px' }}>
                            {viewNic.nic.toUpperCase() === viewNic.extractedNicNumber.toUpperCase() ? <ShieldCheck size={18}/> : <AlertTriangle size={18}/>}
                            {viewNic.nic.toUpperCase() === viewNic.extractedNicNumber.toUpperCase() ? 'OCR Validation: Match' : 'OCR Validation: Mismatch'}
                        </div>
                        <div style={{ fontSize: '14px', color: '#cbd5e1' }}>
                            Entered NIC: <strong>{viewNic.nic}</strong><br/>
                            Extracted from Image: <strong>{viewNic.extractedNicNumber}</strong>
                        </div>
                    </div>
                ) : viewNic.extractedNicNumber === "NOT_FOUND" ? (
                    <div style={{ padding: '12px', background: 'rgba(245, 158, 11, 0.1)', border: '1px solid #f59e0b', borderRadius: '8px', margin: '15px 0 0 0' }}>
                        <div style={{ fontWeight: 'bold', color: '#f59e0b', marginBottom: '5px', display: 'flex', alignItems: 'center', gap: '5px' }}>
                            <AlertTriangle size={18}/> OCR Extraction Failed
                        </div>
                        <div style={{ fontSize: '14px', color: '#cbd5e1' }}>No recognizable NIC number was found in the image.</div>
                    </div>
                ) : (
                    <div style={{ padding: '12px', background: 'rgba(59, 130, 246, 0.1)', border: '1px solid #3b82f6', borderRadius: '8px', margin: '15px 0 0 0' }}>
                        <div style={{ fontWeight: 'bold', color: '#3b82f6', marginBottom: '5px', display: 'flex', alignItems: 'center', gap: '5px' }}>
                            <Activity size={18} className="spin-icon" /> Analyzing Image...
                        </div>
                        <div style={{ fontSize: '14px', color: '#cbd5e1' }}>Running live optical character recognition...</div>
                    </div>
                )}

                <div style={{display: 'flex', flexDirection: 'column', gap: '15px', marginTop: '15px'}}>
                    <div>
                        <h4 style={{marginBottom: '8px', color: '#cbd5e1', fontSize: '15px', fontWeight: 600}}>Front Image</h4>
                        {renderImg(viewNic.nicFrontImageBase64)}
                    </div>
                    <div>
                        <h4 style={{marginBottom: '8px', color: '#cbd5e1', fontSize: '15px', fontWeight: 600}}>Back Image</h4>
                        {renderImg(viewNic.nicBackImageBase64)}
                    </div>
                </div>
                <div className="form-actions" style={{marginTop: '24px', display: 'flex', gap: '10px'}}>
                    {st === "Pending" && !(viewNic.isDrpVerified || viewNic.IsDrpVerified) && (
                        <>
                        <button 
                            className="btn-success" 
                            onClick={() => act(viewNic.nic, "verify-nic")} 
                            style={{flex: 1, padding: '12px', fontSize: '15px', cursor: 'pointer', borderRadius: '8px'}}
                        >
                            Verify NIC
                        </button>
                        <button 
                            className="btn-delete" 
                            onClick={() => {
                                if (window.confirm("Are you sure you want to reject this NIC?")) {
                                    act(viewNic.nic, "reject");
                                }
                            }} 
                            style={{flex: 1, padding: '12px', fontSize: '15px', cursor: 'pointer', borderRadius: '8px'}}
                        >
                            Reject NIC
                        </button>
                        </>
                    )}
                    {st === "Pending" && (viewNic.isDrpVerified || viewNic.IsDrpVerified) && (
                        <div style={{ flex: 1, padding: '12px', fontSize: '15px', borderRadius: '8px', background: 'rgba(16, 185, 129, 0.1)', color: '#10b981', border: '1px solid #10b981', textAlign: 'center', fontWeight: 'bold' }}>
                            <ShieldCheck size={18} style={{ verticalAlign: 'middle', marginRight: '5px', marginBottom: '2px' }} />
                            NIC Verified
                        </div>
                    )}
                    <button onClick={() => { setViewNic(null); setModalAction(""); }} style={{background: "#475569", flex: 1, padding: '12px', fontSize: '15px', borderRadius: '8px', color: 'white', border: 'none', cursor: 'pointer'}}>Close</button>
                </div>
            </div>
        </div>
    )}
    {warningModal && (
        <div className="modal-overlay" onClick={() => { setWarningModal(null); setSolar({...solar, [warningModal.nic]: false}); }}>
            <div className="modal-card" onClick={e => e.stopPropagation()} style={{maxWidth: "450px"}}>
                <div className="modal-header">
                    <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#f59e0b' }}><AlertTriangle size={22}/> Warning</h2>
                    <button className="modal-close-btn" onClick={() => { setWarningModal(null); setSolar({...solar, [warningModal.nic]: false}); }}><X size={20}/></button>
                </div>
                <div style={{ padding: '15px 0', fontSize: '15px', color: '#e2e8f0', lineHeight: 1.5 }}>
                    You have verified both NIC and Solar panel installation for <strong>{warningModal.fullName}</strong>.
                    <br/><br/>
                    Is that okay to make this person as Active?
                </div>
                <div className="form-actions" style={{marginTop: '20px', display: 'flex', gap: '10px'}}>
                    <button 
                        className="btn-success" 
                        onClick={() => {
                            act(warningModal.nic, "activate");
                            setWarningModal(null);
                        }} 
                        style={{flex: 1, padding: '12px', fontSize: '15px', borderRadius: '8px'}}
                    >
                        OK (Activate)
                    </button>
                    <button 
                        className="btn-delete" 
                        onClick={() => {
                            act(warningModal.nic, "reject");
                            setWarningModal(null);
                        }} 
                        style={{flex: 1, padding: '12px', fontSize: '15px', borderRadius: '8px'}}
                    >
                        Reject
                    </button>
                </div>
            </div>
        </div>
    )}
    </>;
}
