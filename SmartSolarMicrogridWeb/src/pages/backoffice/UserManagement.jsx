import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as userService from "../../services/userService";
import OperatorBranchAssignments from "../../components/backoffice/OperatorBranchAssignments";

export default function UserMgmt() {
    const empty = { username: "", password: "", confirmPassword: "", role: 1, status: 0 };
    const [f, setF] = useState(empty), [a, setA] = useState([]), [e, setE] = useState(""), [filter, setFilter] = useState("All Roles"), [edit, setEdit] = useState(null);
    const load = () => userService.getInternalUsers().then(x => setA(x.data));
    useEffect(() => { load(); }, []);
    async function saveF(x) {
        x.preventDefault();
        if (f.password !== f.confirmPassword) return setE("Passwords do not match");
        try {
            if (edit) {
                await userService.updateUser(edit, { role: +f.role, status: +f.status, ...(f.password ? { password: f.password } : {}) });
            } else {
                await userService.createUser({ username: f.username, password: f.password, role: +f.role, status: +f.status });
            }
            setF(empty); setEdit(null); setE(""); load();
        } catch (err) {
            setE(err.response?.data?.error || "Failed to save user");
        }
    }
    async function del(id) {
        if (window.confirm("Are you sure you want to delete this user?")) {
            try { await userService.deleteUser(id); load(); }
            catch (err) { setE(err.response?.data?.error || "Failed to delete"); }
        }
    }
    function startEdit(u) {
        setEdit(u.id);
        setF({ username: u.username, password: "", confirmPassword: "", role: u.role, status: u.status });
        setE("");
    }
    const fa = filter === "All Roles" ? a : a.filter(u => u.role === (filter === "Backoffice" ? 0 : 1));
    return <><Title t="User Management" d="Manage internal Backoffice and Grid Operator accounts" /><div className="grid2"><form className="card" onSubmit={saveF}><h2>{edit ? "Edit User" : "Create User"}</h2>{e && <div className="err">{e}</div>}<label>Username<input required={!edit} disabled={!!edit} value={f.username} onChange={e => setF({ ...f, username: e.target.value })} /></label><label>Password<input required={!edit} type="password" value={f.password} placeholder={edit ? "Leave blank to keep current" : ""} onChange={e => setF({ ...f, password: e.target.value })} /></label><label>Confirm Password<input required={!edit && f.password} type="password" value={f.confirmPassword} onChange={e => setF({ ...f, confirmPassword: e.target.value })} /></label><label>Role<select value={f.role} onChange={e => setF({ ...f, role: e.target.value })}><option value={0}>Backoffice</option><option value={1}>Grid Operator</option></select></label><label>Status<select value={f.status} onChange={e => setF({ ...f, status: e.target.value })}><option value={0}>Active</option><option value={1}>Inactive</option></select></label><div className="form-actions"><button type="submit" style={{flex:1}}>{edit ? "Update User" : "Create User"}</button>{edit && <button type="button" onClick={() => { setEdit(null); setF(empty); setE(""); }} style={{background:"#94a3b8",flex:1}}>Cancel</button>}</div></form><Table><div className="table-header"><h2 style={{ margin: 0 }}>Internal Users</h2><select value={filter} onChange={e => setFilter(e.target.value)} style={{ padding: "6px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", width: "auto", marginTop: 0, fontWeight: 600, color: "#475569", cursor: "pointer" }}><option>All Roles</option><option>Backoffice</option><option>Grid Operator</option></select></div><table><thead><tr><th>Username</th><th>Role</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody>{fa.length > 0 ? fa.map(u => <tr key={u.id}><td>{u.username}</td><td>{u.role === 0 ? "Backoffice" : "Grid Operator"}</td><td>{u.status === 0 ? "Active" : "Inactive"}</td><td>{new Date(u.createdAt).toLocaleDateString()}</td><td><button className="btn-edit" onClick={() => startEdit(u)}>Edit</button><button className="btn-delete" onClick={() => del(u.id)}>Delete</button></td></tr>) : <tr><td colSpan="5" style={{ textAlign: "center", padding: "20px", color: "#64748b" }}>No users found for this role.</td></tr>}</tbody></table></Table></div><OperatorBranchAssignments /></>;
}
