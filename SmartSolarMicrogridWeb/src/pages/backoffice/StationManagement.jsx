import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as stationService from "../../services/stationService";

function LocationPickerModal({ initialLat, initialLng, initialName, isOpen, onClose, onSelectLocation }) {
    const [searchQuery, setSearchQuery] = useState("");
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [searchError, setSearchError] = useState("");
    const [isMapReady, setIsMapReady] = useState(false);
    const [selectedCoords, setSelectedCoords] = useState([
        initialLat && !isNaN(+initialLat) ? +initialLat : 6.9271,
        initialLng && !isNaN(+initialLng) ? +initialLng : 79.8612
    ]);
    const [selectedPlaceName, setSelectedPlaceName] = useState(initialName || "");
    const [isLocating, setIsLocating] = useState(false);

    const mapContainerRef = useRef(null);
    const mapInstanceRef = useRef(null);
    const markerRef = useRef(null);

    // Instant local Sri Lanka & Renewable Energy Hubs dataset for zero-latency search
    const popularLocations = [
        { name: "Hambantota Solar Park (100MW)", lat: 6.1429, lng: 81.1212, category: "Solar Park" },
        { name: "Mirijjawila Solar Power Station", lat: 6.1685, lng: 81.0820, category: "Solar Park" },
        { name: "Siyambalanduwa Renewable Hub", lat: 6.9064, lng: 81.5542, category: "Solar Station" },
        { name: "Pooneryn Renewable Energy Park", lat: 9.5028, lng: 80.2014, category: "Solar & Wind Hub" },
        { name: "Maduru Oya Floating Solar Facility", lat: 7.6481, lng: 81.2185, category: "Floating Solar" },
        { name: "Colombo Central Solar Hub", lat: 6.9271, lng: 79.8612, category: "Urban Hub" },
        { name: "Colombo Fort Grid Substation", lat: 6.9344, lng: 79.8428, category: "Grid Station" },
        { name: "Kandy Central Solar Station", lat: 7.2906, lng: 80.6337, category: "Regional Hub" },
        { name: "Galle Southern Microgrid", lat: 6.0535, lng: 80.2210, category: "Regional Hub" },
        { name: "Anuradhapura Solar Node", lat: 8.3114, lng: 80.4037, category: "North Central Hub" },
        { name: "Jaffna Peninsula Solar Hub", lat: 9.6615, lng: 80.0255, category: "Northern Hub" },
        { name: "Trincomalee Deep Bay Station", lat: 8.5874, lng: 81.2152, category: "Eastern Hub" },
        { name: "Kurunegala Wayamba Solar Hub", lat: 7.4863, lng: 80.3623, category: "Regional Hub" },
        { name: "Negombo Coastal Solar Node", lat: 7.2008, lng: 79.8736, category: "Coastal Node" },
        { name: "Matara Southern Grid Station", lat: 5.9549, lng: 80.5550, category: "Regional Hub" },
        { name: "Batticaloa East Coast Solar", lat: 7.7310, lng: 81.6747, category: "Regional Hub" },
        { name: "Polonnaruwa Ancient City Solar", lat: 7.9403, lng: 81.0188, category: "North Central Node" },
        { name: "Badulla Uva Solar Station", lat: 6.9934, lng: 81.0550, category: "Uva Hub" },
        { name: "Ratnapura Sabaragamuwa Grid", lat: 6.6828, lng: 80.4010, category: "Regional Hub" },
        { name: "Nuwara Eliya Hill Station", lat: 6.9497, lng: 80.7891, category: "Central Grid" },
        { name: "Puttalam Wind & Solar Complex", lat: 8.0362, lng: 79.8283, category: "Renewable Hub" },
        { name: "Mannar Island Energy Node", lat: 8.9810, lng: 79.9042, category: "Renewable Hub" }
    ];

    const presetRegions = [
        { name: "Colombo", lat: 6.9271, lng: 79.8612 },
        { name: "Hambantota", lat: 6.1429, lng: 81.1212 },
        { name: "Kandy", lat: 7.2906, lng: 80.6337 },
        { name: "Galle", lat: 6.0535, lng: 80.2210 },
        { name: "Anuradhapura", lat: 8.3114, lng: 80.4037 },
        { name: "Jaffna", lat: 9.6615, lng: 80.0255 },
        { name: "Trincomalee", lat: 8.5874, lng: 81.2152 }
    ];

    // Multi-CDN Leaflet dynamic loader
    const ensureLeaflet = async () => {
        if (window.L && typeof window.L.map === "function") {
            return window.L;
        }
        try {
            const L = await import("leaflet");
            window.L = L.default || L;
            return window.L;
        } catch (err) {
            console.error("Failed to load leaflet", err);
            return null;
        }
    };

    useEffect(() => {
        if (!isOpen) return;

        const lat = initialLat && !isNaN(+initialLat) ? +initialLat : 6.9271;
        const lng = initialLng && !isNaN(+initialLng) ? +initialLng : 79.8612;
        setSelectedCoords([lat, lng]);
        setSelectedPlaceName(initialName || "");
        setSearchQuery("");
        setSearchResults([]);
        setSearchError("");

        let active = true;

        ensureLeaflet().then((L) => {
            if (!active || !L || !mapContainerRef.current) return;

            try {
                if (mapContainerRef.current._leaflet_id) {
                    delete mapContainerRef.current._leaflet_id;
                }

                if (mapInstanceRef.current) {
                    mapInstanceRef.current.remove();
                    mapInstanceRef.current = null;
                }

                const createPin = () => {
                    return L.divIcon({
                        className: 'custom-map-marker-pin',
                        html: `<div style="background:#10b981;width:26px;height:26px;border-radius:50%;border:3px solid #ffffff;box-shadow:0 0 20px rgba(16,185,129,0.9);display:flex;align-items:center;justify-content:center;cursor:pointer;"><div style="background:#ffffff;width:8px;height:8px;border-radius:50%;"></div></div>`,
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                };

                const map = L.map(mapContainerRef.current, {
                    center: [lat, lng],
                    zoom: 12,
                    zoomControl: true,
                    attributionControl: true
                });

                L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                    maxZoom: 19,
                    attribution: '&copy; OpenStreetMap'
                }).addTo(map);

                const marker = L.marker([lat, lng], {
                    draggable: true,
                    icon: createPin()
                }).addTo(map);

                marker.on("dragend", (e) => {
                    const pos = e.target.getLatLng();
                    const newLat = Number(pos.lat.toFixed(6));
                    const newLng = Number(pos.lng.toFixed(6));
                    setSelectedCoords([newLat, newLng]);
                });

                map.on("click", (e) => {
                    const newLat = Number(e.latlng.lat.toFixed(6));
                    const newLng = Number(e.latlng.lng.toFixed(6));
                    setSelectedCoords([newLat, newLng]);
                    marker.setLatLng([newLat, newLng]);
                });

                mapInstanceRef.current = map;
                markerRef.current = marker;
                setIsMapReady(true);

                // Multiple invalidation passes to prevent grey map tiles
                setTimeout(() => map && map.invalidateSize(), 150);
                setTimeout(() => map && map.invalidateSize(), 350);
            } catch (err) {
                console.error("Map initialization error:", err);
            }
        });

        return () => {
            active = false;
        };
    }, [isOpen, initialLat, initialLng, initialName]);

    useEffect(() => {
        if (!isOpen) {
            setIsMapReady(false);
            if (mapInstanceRef.current) {
                mapInstanceRef.current.remove();
                mapInstanceRef.current = null;
                markerRef.current = null;
            }
        }
    }, [isOpen]);

    const updateMapPosition = (lat, lng, zoom = 14, name = "") => {
        const fixedLat = Number(Number(lat).toFixed(6));
        const fixedLng = Number(Number(lng).toFixed(6));
        setSelectedCoords([fixedLat, fixedLng]);
        if (name) setSelectedPlaceName(name);

        if (mapInstanceRef.current) {
            mapInstanceRef.current.setView([fixedLat, fixedLng], zoom);
            if (markerRef.current) {
                markerRef.current.setLatLng([fixedLat, fixedLng]);
            }
            setTimeout(() => mapInstanceRef.current?.invalidateSize(), 100);
        }
    };

    // Live search combining instant local database + Nominatim API
    const handleSearch = async (e) => {
        if (e) e.preventDefault();
        const query = searchQuery.trim().toLowerCase();
        if (!query) return;

        setIsSearching(true);
        setSearchError("");

        // 1. Instant local matching
        const localMatches = popularLocations.filter(loc => 
            loc.name.toLowerCase().includes(query) || 
            loc.category.toLowerCase().includes(query)
        ).map(loc => ({
            display_name: `${loc.name} (${loc.category})`,
            lat: loc.lat.toString(),
            lon: loc.lng.toString(),
            isLocal: true
        }));

        setSearchResults(localMatches);

        // 2. Fetch live global/regional geocoding
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 6000);

            const res = await fetch(
                `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=6`,
                { signal: controller.signal }
            );
            clearTimeout(timeoutId);

            if (res.ok) {
                const apiData = await res.json();
                if (Array.isArray(apiData) && apiData.length > 0) {
                    const combined = [...localMatches];
                    apiData.forEach(item => {
                        if (!combined.some(c => Math.abs(parseFloat(c.lat) - parseFloat(item.lat)) < 0.01 && Math.abs(parseFloat(c.lon) - parseFloat(item.lon)) < 0.01)) {
                            combined.push(item);
                        }
                    });
                    setSearchResults(combined);
                } else if (localMatches.length === 0) {
                    setSearchError("No locations found for this query. Try a city or district name (e.g. Kandy, Hambantota, Galle).");
                }
            } else if (localMatches.length === 0) {
                setSearchError("No results found. Please check spelling or select from quick hubs below.");
            }
        } catch (err) {
            if (localMatches.length === 0) {
                setSearchError("Could not reach online geocoding. Please pick a location from the map or quick hubs.");
            }
        } finally {
            setIsSearching(false);
        }
    };

    // Live auto-filter as user types
    const handleInputChange = (e) => {
        const val = e.target.value;
        setSearchQuery(val);
        if (!val.trim()) {
            setSearchResults([]);
            setSearchError("");
            return;
        }

        const query = val.toLowerCase().trim();
        const instantMatches = popularLocations.filter(loc => 
            loc.name.toLowerCase().includes(query) || 
            loc.category.toLowerCase().includes(query)
        ).map(loc => ({
            display_name: `${loc.name} (${loc.category})`,
            lat: loc.lat.toString(),
            lon: loc.lng.toString(),
            isLocal: true
        }));

        if (instantMatches.length > 0) {
            setSearchResults(instantMatches);
            setSearchError("");
        }
    };

    const selectSearchResult = (item) => {
        const lat = parseFloat(item.lat);
        const lon = parseFloat(item.lon);
        const shortName = item.display_name.split(",")[0].split("(")[0].trim();
        updateMapPosition(lat, lon, 14, shortName);
        setSearchResults([]);
        setSearchQuery(item.display_name.split(",")[0]);
    };

    const detectCurrentLocation = () => {
        if (!navigator.geolocation) {
            setSearchError("Geolocation is not supported by your browser.");
            return;
        }
        setIsLocating(true);
        setSearchError("");
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                const lat = pos.coords.latitude;
                const lng = pos.coords.longitude;
                updateMapPosition(lat, lng, 15, "Current GPS Device Location");
                setIsLocating(false);
            },
            (err) => {
                setIsLocating(false);
                let msg = "Could not retrieve GPS location.";
                if (err.code === 1) msg = "Location permission denied in browser.";
                else if (err.code === 2) msg = "Position unavailable from GPS.";
                else if (err.code === 3) msg = "GPS location request timed out.";
                setSearchError(msg);
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    };

    const handleConfirm = () => {
        onSelectLocation({
            latitude: selectedCoords[0],
            longitude: selectedCoords[1],
            placeName: selectedPlaceName
        });
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="map-picker-modal" onClick={e => e.stopPropagation()}>
                <div className="map-picker-header">
                    <div className="map-picker-title">
                        <Compass size={24} style={{ color: "var(--accent-primary)" }} />
                        <div>
                            <h3>Select Node Location</h3>
                            <p>Search any city/address or click anywhere on the map to position the microgrid pin</p>
                        </div>
                    </div>
                    <button className="modal-close-btn" onClick={onClose} aria-label="Close">
                        <X size={20} />
                    </button>
                </div>

                <div className="map-picker-body">
                    {/* Search Bar & Auto-Detect Controls */}
                    <div className="map-picker-controls">
                        <form onSubmit={handleSearch} className="map-search-form">
                            <div className="map-search-input-wrap">
                                <Search size={16} />
                                <input
                                    type="text"
                                    placeholder="Search city, town, or solar park (e.g. Hambantota, Kandy, Galle)..."
                                    value={searchQuery}
                                    onChange={handleInputChange}
                                />
                                {searchQuery && (
                                    <button
                                        type="button"
                                        className="search-clear-btn"
                                        onClick={() => { setSearchQuery(""); setSearchResults([]); }}
                                    >
                                        <X size={14} />
                                    </button>
                                )}
                            </div>
                            <button type="submit" className="map-search-btn" disabled={isSearching}>
                                {isSearching ? <Loader2 size={15} className="spin-icon" /> : "Search"}
                            </button>
                        </form>

                        <button
                            type="button"
                            className="map-my-location-btn"
                            onClick={detectCurrentLocation}
                            disabled={isLocating}
                            title="Center on my current GPS location"
                        >
                            {isLocating ? <Loader2 size={14} className="spin-icon" /> : <Navigation size={14} />}
                            <span>My GPS</span>
                        </button>
                    </div>

                    {/* Search Results Dropdown */}
                    {searchResults.length > 0 && (
                        <div className="map-search-dropdown">
                            <div className="map-search-dropdown-title">Matching Locations (Click to place marker):</div>
                            {searchResults.map((item, idx) => (
                                <div
                                    key={idx}
                                    className="map-search-item"
                                    onClick={() => selectSearchResult(item)}
                                >
                                    <MapPin size={14} style={{ color: "var(--accent-primary)", flexShrink: 0 }} />
                                    <span className="map-search-item-text">{item.display_name}</span>
                                </div>
                            ))}
                        </div>
                    )}

                    {searchError && (
                        <div className="map-error-alert">
                            <AlertCircle size={15} style={{ flexShrink: 0 }} />
                            <span>{searchError}</span>
                        </div>
                    )}

                    {/* Quick Preset Regions */}
                    <div className="preset-chips-container">
                        <span className="preset-label">Quick Hubs:</span>
                        <div className="preset-chips-list">
                            {presetRegions.map(p => (
                                <button
                                    key={p.name}
                                    type="button"
                                    className="preset-region-chip"
                                    onClick={() => updateMapPosition(p.lat, p.lng, 13, p.name)}
                                >
                                    <MapPin size={11} />
                                    {p.name}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Interactive Map Canvas */}
                    <div className="map-canvas-container">
                        <div ref={mapContainerRef} className="leaflet-map-canvas" />
                        <div className="map-hint-badge">
                            <Crosshair size={13} /> Click map or drag marker to set exact coordinates
                        </div>
                    </div>
                </div>

                {/* Bottom Bar */}
                <div className="map-picker-footer">
                    <div className="map-coords-display">
                        <div className="map-coords-row">
                            <span className="coord-label">Latitude:</span>
                            <strong className="coord-value">{selectedCoords[0]}</strong>
                        </div>
                        <div className="map-coords-row">
                            <span className="coord-label">Longitude:</span>
                            <strong className="coord-value">{selectedCoords[1]}</strong>
                        </div>
                        {selectedPlaceName && (
                            <div className="map-coords-row location-tag">
                                <MapPin size={12} style={{ color: "var(--accent-primary)" }} />
                                <span>{selectedPlaceName}</span>
                            </div>
                        )}
                    </div>

                    <div className="map-footer-actions">
                        <button type="button" className="btn-cancel" onClick={onClose}>
                            Cancel
                        </button>
                        <button type="button" className="btn-apply-location" onClick={handleConfirm}>
                            <Check size={16} /> Apply Location
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default function Stations() {
    const [a, setA] = useState([]), [edit, setEdit] = useState(null), [err, setErr] = useState("");
    const [search, setSearch] = useState("");
    const [deactModal, setDeactModal] = useState(null);
    const [deactReason, setDeactReason] = useState("");
    const [modalErr, setModalErr] = useState("");
    const [gpsLoading, setGpsLoading] = useState(false);
    const [gpsStatus, setGpsStatus] = useState(null);
    const [isMapModalOpen, setIsMapModalOpen] = useState(false);

    const getNextCode = (nodes) => {
        if (!nodes || nodes.length === 0) return "N-001";
        const max = Math.max(...nodes.map(n => {
            const match = n.nodeCode.match(/N-(\d+)/);
            return match ? parseInt(match[1]) : 0;
        }));
        return `N-${(max + 1).toString().padStart(3, '0')}`;
    };

    const empty = (nodes = []) => ({ nodeCode: getNextCode(nodes), nodeName: "", latitude: "", longitude: "", capacityKw: "", batterySlotAvailability: "", scheduleStart: "08:00", scheduleEnd: "18:00" });
    const [f, setF] = useState(empty());

    const load = () => stationService.getStations().then(x => {
        setA(x.data);
        if (!edit) setF(empty(x.data));
    });
    useEffect(() => { load(); }, []);

    const getCurrentGpsLocation = () => {
        if (!navigator.geolocation) {
            setGpsStatus({ type: "error", message: "Geolocation is not supported by your browser." });
            return;
        }
        setGpsLoading(true);
        setGpsStatus(null);
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = Number(position.coords.latitude.toFixed(6));
                const lng = Number(position.coords.longitude.toFixed(6));
                const acc = Math.round(position.coords.accuracy);
                setF(prev => ({ ...prev, latitude: lat, longitude: lng }));
                setGpsStatus({
                    type: "success",
                    message: `GPS acquired: ${lat}, ${lng} (Accuracy: ±${acc}m)`
                });
                setGpsLoading(false);
            },
            (error) => {
                let msg = "Failed to retrieve GPS location.";
                if (error.code === error.PERMISSION_DENIED) {
                    msg = "Location permission denied. Please enable location access in browser settings.";
                } else if (error.code === error.POSITION_UNAVAILABLE) {
                    msg = "Location information is unavailable.";
                } else if (error.code === error.TIMEOUT) {
                    msg = "GPS location request timed out. Please retry.";
                }
                setGpsStatus({ type: "error", message: msg });
                setGpsLoading(false);
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 0
            }
        );
    };

    const handleLocationSelected = ({ latitude, longitude, placeName }) => {
        setF(prev => ({
            ...prev,
            latitude,
            longitude,
            nodeName: prev.nodeName ? prev.nodeName : (placeName ? `${placeName} Solar Hub` : prev.nodeName)
        }));
        setGpsStatus({
            type: "success",
            message: `Coordinates updated: ${latitude}, ${longitude}${placeName ? ` (${placeName})` : ''}`
        });
    };

    async function saveF(e) {
        e.preventDefault();
        const b = { 
            ...f, 
            latitude: +f.latitude, 
            longitude: +f.longitude, 
            capacityKw: +f.capacityKw, 
            batterySlotAvailability: +f.batterySlotAvailability,
            scheduleStart: f.scheduleStart || "08:00",
            scheduleEnd: f.scheduleEnd || "18:00"
        };
        try {
            if (edit) await stationService.updateStation(edit, b);
            else await stationService.createStation(b);
            setEdit(null);
            setGpsStatus(null);
            setErr("");
            load();
        } catch (x) {
            setErr(x.response?.data?.error || "Failed to save station");
        }
    }

    const suggestions = [
        "Routine Maintenance",
        "Grid Upgrading / Maintenance",
        "Technical Fault / Breakdown",
        "Emergency Safety Shutdown",
        "Adverse Weather / Inspection"
    ];

    function openDeactivateModal(node) {
        setDeactModal(node);
        setDeactReason("Routine Maintenance");
        setModalErr("");
    }

    async function confirmDeactivate(e) {
        e.preventDefault();
        if (!deactReason.trim()) {
            setModalErr("Admin note / reason is required for deactivation.");
            return;
        }
        try {
            await stationService.deactivateStation(deactModal.id, deactReason.trim());
            setDeactModal(null);
            setDeactReason("");
            setModalErr("");
            setErr("");
            load();
        } catch (x) {
            setModalErr(x.response?.data?.error || "Cannot deactivate node");
        }
    }

    async function activate(id) {
        try {
            await stationService.activateStation(id);
            setErr("");
            load();
        } catch (x) {
            setErr(x.response?.data?.error || "Cannot activate node");
        }
    }

    const filteredNodes = a.filter(x => {
        if (!search.trim()) return true;
        const q = search.toLowerCase();
        return (
            x.nodeCode?.toLowerCase().includes(q) ||
            x.nodeName?.toLowerCase().includes(q) ||
            (x.status === 0 || x.status === "Active" ? "active" : "inactive").includes(q) ||
            x.adminNote?.toLowerCase().includes(q) ||
            x.deactivationReason?.toLowerCase().includes(q) ||
            `${x.latitude},${x.longitude}`.includes(q)
        );
    });

    const inactiveMatches = search.trim() ? filteredNodes.filter(x => x.status !== 0) : [];

    const hasCoords = f.latitude !== "" && f.longitude !== "" && !isNaN(+f.latitude) && !isNaN(+f.longitude);

    return <>
        <Title t="Microgrid Nodes" d="Register and manage solar grid hubs" />
        <div className="grid2">
            <form className="card" onSubmit={saveF}>
                <h2>{edit ? "Update" : "Register"} Station</h2>
                {err && <div className="err">{err}</div>}
                
                <label>
                    Node Code
                    <input
                        required
                        disabled={!!edit}
                        value={f.nodeCode || ''}
                        type="text"
                        placeholder="e.g. N-003"
                        onChange={e => setF({ ...f, nodeCode: e.target.value })}
                    />
                </label>

                <label>
                    Branch / Station Name
                    <input
                        required
                        value={f.nodeName || ''}
                        type="text"
                        placeholder="e.g. Colombo Central Solar Hub"
                        onChange={e => setF({ ...f, nodeName: e.target.value })}
                    />
                </label>

                {/* GPS Location Section */}
                <div className="gps-section">
                    <div className="gps-section-header">
                        <span className="gps-section-title">
                            <Crosshair size={16} /> Location Coordinates
                        </span>
                        <div className="gps-actions-row">
                            <button
                                type="button"
                                className="gps-btn map-pick-btn"
                                onClick={() => setIsMapModalOpen(true)}
                                title="Open interactive map to search any location or click to pick coordinates"
                            >
                                <Map size={14} /> Map Picker & Search
                            </button>
                            <button
                                type="button"
                                className="gps-btn"
                                disabled={gpsLoading}
                                onClick={getCurrentGpsLocation}
                                title="Auto-detect current device GPS location"
                            >
                                {gpsLoading ? (
                                    <>
                                        <Loader2 size={14} className="spin-icon" /> Locating...
                                    </>
                                ) : (
                                    <>
                                        <Navigation size={14} /> My GPS
                                    </>
                                )}
                            </button>
                        </div>
                    </div>

                    {gpsStatus && (
                        <div className={`gps-status-box ${gpsStatus.type}`}>
                            {gpsStatus.type === "success" ? (
                                <CheckCircle2 size={15} style={{ flexShrink: 0 }} />
                            ) : (
                                <AlertTriangle size={15} style={{ flexShrink: 0 }} />
                            )}
                            <span>{gpsStatus.message}</span>
                        </div>
                    )}

                    <div className="gps-coords-grid">
                        <label>
                            Latitude
                            <input
                                required
                                value={f.latitude !== undefined ? f.latitude : ''}
                                type="number"
                                min="-90"
                                max="90"
                                step="0.000001"
                                placeholder="e.g. 6.927100"
                                onChange={e => setF({ ...f, latitude: e.target.value })}
                            />
                        </label>
                        <label>
                            Longitude
                            <input
                                required
                                value={f.longitude !== undefined ? f.longitude : ''}
                                type="number"
                                min="-180"
                                max="180"
                                step="0.000001"
                                placeholder="e.g. 79.861200"
                                onChange={e => setF({ ...f, longitude: e.target.value })}
                            />
                        </label>
                    </div>

                    {hasCoords && (
                        <div className="gps-preview-bar">
                            <a
                                href={`https://www.google.com/maps?q=${f.latitude},${f.longitude}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="gps-map-link"
                            >
                                <ExternalLink size={13} /> View on Google Maps ({f.latitude}, {f.longitude})
                            </a>
                        </div>
                    )}
                </div>

                <label>
                    Capacity (kW)
                    <input
                        required
                        value={f.capacityKw || ''}
                        type="number"
                        min="0.1"
                        step="0.1"
                        placeholder="e.g. 150"
                        onChange={e => setF({ ...f, capacityKw: e.target.value })}
                    />
                </label>

                <label>
                    Battery Slot Availability
                    <input
                        required
                        value={f.batterySlotAvailability !== undefined ? f.batterySlotAvailability : ''}
                        type="number"
                        min="0"
                        step="1"
                        placeholder="e.g. 20"
                        onChange={e => setF({ ...f, batterySlotAvailability: e.target.value })}
                    />
                </label>

                <div className="form-actions">
                    <button type="submit" style={{flex:1}}> {edit ? "Update" : "Create Node"} </button>
                    {edit && <button type="button" onClick={() => { setEdit(null); setF(empty(a)); setGpsStatus(null); setErr(""); }} style={{background:"#94a3b8",flex:1}}>Cancel</button>}
                </div>
            </form>

            <LocationPickerModal
                isOpen={isMapModalOpen}
                onClose={() => setIsMapModalOpen(false)}
                initialLat={f.latitude}
                initialLng={f.longitude}
                initialName={f.nodeName}
                onSelectLocation={handleLocationSelected}
            />

            <Table>
                <div className="table-header">
                    <h2 style={{ margin: 0 }}>Registered Nodes</h2>
                    <div className="table-search-bar">
                        <Search size={15}/>
                        <input
                            type="text"
                            placeholder="Search branch name, code, note..."
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

                {inactiveMatches.length > 0 && (
                    <div className="inactive-search-alert">
                        <AlertTriangle size={20}/>
                        <div>
                            <strong>Deactivated Branch Notice:</strong>
                            {inactiveMatches.map(n => (
                                <div key={n.id} style={{ marginTop: '3px', fontSize: '13px' }}>
                                    Hub <strong>{n.nodeCode} ({n.nodeName})</strong> is currently <u>Unavailable</u>.
                                    <br/>
                                    <strong>Admin Reason:</strong> {n.adminNote || n.deactivationReason || "Routine Maintenance"}
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                <table>
                    <thead>
                        <tr>
                            <th>Code</th>
                            <th>Name</th>
                            <th>GPS Coordinates</th>
                            <th>Capacity</th>
                            <th>Battery</th>
                            <th>Status</th>
                            <th>Admin Note</th>
                            <th style={{ textAlign: "right", paddingRight: "18px" }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredNodes.length > 0 ? filteredNodes.map(x => (
                            <tr key={x.id}>
                                <td><strong>{x.nodeCode}</strong></td>
                                <td>{x.nodeName}</td>
                                <td>
                                    <a
                                        href={`https://www.google.com/maps?q=${x.latitude},${x.longitude}`}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="table-gps-link"
                                        title="Click to view location in Google Maps"
                                    >
                                        <MapPin size={13} className="gps-pin-icon" />
                                        <span>{x.latitude}, {x.longitude}</span>
                                        <ExternalLink size={11} className="gps-ext-icon" />
                                    </a>
                                </td>
                                <td>{x.capacityKw} kW</td>
                                <td>{x.batterySlotAvailability}</td>
                                <td>
                                    <span className={`badge ${x.status === 0 || x.status === "Active" ? "badge-active" : "badge-inactive"}`}>
                                        {x.status === 0 || x.status === "Active" ? "Active" : "Inactive"}
                                    </span>
                                </td>
                                <td>
                                    {x.adminNote || x.deactivationReason || (x.status !== 0 && x.status !== "Active") ? (
                                        <span className="reason-pill" style={x.status === 0 || x.status === "Active" ? { background: "var(--input-bg)", color: "var(--text-muted)", borderColor: "var(--border-color)" } : {}} title={`Admin Note: ${x.adminNote || x.deactivationReason || "Routine Maintenance"}`}>
                                            {x.status !== 0 && x.status !== "Active" && <AlertTriangle size={12} style={{ flexShrink: 0 }}/>}
                                            <span>{x.adminNote || x.deactivationReason || "Routine Maintenance"}</span>
                                        </span>
                                    ) : (
                                        <span className="text-muted-dash">—</span>
                                    )}
                                </td>
                                <td>
                                    <div className="table-actions-cell" style={{ justifyContent: "flex-end" }}>
                                        <button className="btn-edit" onClick={() => { setEdit(x.id); setF(x); setGpsStatus(null); }}>Edit</button>
                                        {x.status === 0 || x.status === "Active" ? (
                                            <button className="btn-delete" onClick={() => openDeactivateModal(x)}>Deactivate</button>
                                        ) : (
                                            <button className="btn-success" onClick={() => activate(x.id)}>Activate</button>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        )) : (
                            <tr>
                                <td colSpan="8" style={{ textAlign: "center", padding: "24px", color: "#94a3b8" }}>
                                    No microgrid nodes matched your search.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </Table>
        </div>

        {/* Deactivation Modal with Mandatory Admin Note & Suggestions */}
        {deactModal && (
            <div className="modal-overlay" onClick={() => setDeactModal(null)}>
                <div className="modal-card" onClick={e => e.stopPropagation()}>
                    <div className="modal-header">
                        <h2>
                            <AlertTriangle size={22}/>
                            Deactivate Microgrid Node
                        </h2>
                        <button className="modal-close-btn" onClick={() => setDeactModal(null)}>
                            <X size={20}/>
                        </button>
                    </div>

                    <div className="modal-info-box">
                        Deactivating <strong>{deactModal.nodeCode} — {deactModal.nodeName}</strong> will immediately set the hub to Inactive and automatically lock all related Energy Slots to <u>Unavailable</u>.
                    </div>

                    {modalErr && <div className="err" style={{ margin: "0 0 14px" }}>{modalErr}</div>}

                    <form onSubmit={confirmDeactivate}>
                        <span className="suggestion-chips-label">Quick Suggestions (Click to set reason):</span>
                        <div className="suggestion-chips">
                            {suggestions.map(s => (
                                <button
                                    key={s}
                                    type="button"
                                    className={`suggestion-chip ${deactReason === s ? "active" : ""}`}
                                    onClick={() => setDeactReason(s)}
                                >
                                    {s}
                                </button>
                            ))}
                        </div>

                        <label style={{ display: "block", marginTop: 0 }}>
                            <span style={{ fontWeight: 600, color: "#f8fafc" }}>Admin Note / Deactivation Reason * (Required):</span>
                            <textarea
                                required
                                rows={3}
                                style={{ width: "100%", marginTop: "8px", resize: "vertical" }}
                                placeholder="Enter reason (e.g., Routine Maintenance, Grid Repair, etc.)..."
                                value={deactReason}
                                onChange={e => setDeactReason(e.target.value)}
                            />
                        </label>

                        <div className="form-actions" style={{ marginTop: "20px" }}>
                            <button
                                type="submit"
                                className="btn-delete"
                                style={{ flex: 1, padding: "12px", fontSize: "14px", borderRadius: "12px" }}
                            >
                                Confirm Deactivation
                            </button>
                            <button
                                type="button"
                                onClick={() => setDeactModal(null)}
                                style={{ background: "#475569", flex: 1, padding: "12px", fontSize: "14px", borderRadius: "12px" }}
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        )}
    </>;
}
