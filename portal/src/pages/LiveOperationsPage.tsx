import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { api } from '../api/client';
import { useLive, type LiveEventName } from '../api/live';
import type { Agent, Ambulance } from '../types';

interface FeedItem { id: number; name: LiveEventName; text: string; sub: string; emergency?: boolean; at: number; }

interface GeoPoint { lat: number; lng: number; label: string; }
interface DriverPoint { agentId: number; name: string; lat: number | null; lng: number | null; status: string; }
interface RouteDto {
  caseId: number; caseNumber: string; patientName: string;
  pickup: GeoPoint | null; destination: GeoPoint | null; driver: DriverPoint | null; assignmentStatus: string | null;
}
interface Journey { assignmentId: number; caseId: number; caseNumber: string; patientName: string; agentId: number; agentName: string; status: string; }

const EVENT_COLOR: Record<string, string> = {
  event: 'var(--primary)', location: 'var(--accent)', notification: 'var(--warning)', request: 'var(--danger)',
};

const PICKUP_COLOR = '#16a34a';   // green
const HOSPITAL_COLOR = '#dc2626'; // red
const DRIVER_COLOR = '#2563eb';   // blue

let feedSeq = 0;

// A labelled pin using a Leaflet divIcon so pickup / hospital / ambulance read clearly.
function pin(color: string, glyph: string): L.DivIcon {
  return L.divIcon({
    className: 'route-pin',
    html: `<div style="background:${color};width:30px;height:30px;border-radius:50% 50% 50% 0;transform:rotate(-45deg);border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center">
             <span style="transform:rotate(45deg);font-size:15px;line-height:1">${glyph}</span>
           </div>`,
    iconSize: [30, 30], iconAnchor: [15, 28], tooltipAnchor: [0, -26],
  });
}

export default function LiveOperationsPage() {
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [connected, setConnected] = useState(false);
  const [journeys, setJourneys] = useState<Journey[]>([]);
  const [selectedCase, setSelectedCase] = useState<number | null>(null);
  const [routeInfo, setRouteInfo] = useState<RouteDto | null>(null);

  const mapRef = useRef<L.Map | null>(null);
  const markers = useRef<Record<string, L.CircleMarker>>({});
  const mapEl = useRef<HTMLDivElement | null>(null);

  // Route overlay refs
  const routeLayer = useRef<L.LayerGroup | null>(null);
  const driverMarker = useRef<L.Marker | null>(null);
  const trackedAgentId = useRef<number | null>(null);

  // Init map once
  useEffect(() => {
    if (mapRef.current || !mapEl.current) return;
    const map = L.map(mapEl.current, { zoomControl: true }).setView([12.9716, 77.5946], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap', maxZoom: 19,
    }).addTo(map);
    mapRef.current = map;
    routeLayer.current = L.layerGroup().addTo(map);

    // Seed initial vehicle positions
    Promise.all([
      api.get<Agent[]>('/agents').catch(() => ({ data: [] as Agent[] })),
      api.get<Ambulance[]>('/ambulances/tracking').catch(() => ({ data: [] as Ambulance[] })),
    ]).then(([ag, amb]) => {
      ag.data.forEach((a) => { if (a.currentLatitude != null && a.status !== 'OFFLINE') upsertMarker(`agent-${a.id}`, a.currentLatitude!, a.currentLongitude!, a.fullName, DRIVER_COLOR); });
      amb.data.forEach((a) => { if (a.currentLatitude != null) upsertMarker(`ambulance-${a.id}`, a.currentLatitude!, a.currentLongitude!, a.registrationNo, HOSPITAL_COLOR); });
    });

    return () => { map.remove(); mapRef.current = null; markers.current = {}; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Load active journeys and default-select the first one
  useEffect(() => {
    loadJourneys();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadJourneys() {
    try {
      const { data } = await api.get<{ journeys: Journey[] }>('/live/active-journeys');
      setJourneys(data.journeys);
      setSelectedCase((cur) => cur ?? (data.journeys[0]?.caseId ?? null));
    } catch { /* ignore */ }
  }

  // Draw the route whenever the selected case changes
  useEffect(() => {
    if (selectedCase == null) return;
    drawRoute(selectedCase);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCase]);

  function upsertMarker(key: string, lat: number, lng: number, label: string, color: string) {
    const map = mapRef.current;
    if (!map) return;
    // The tracked driver is the ambulance pin — never draw a generic dot for it (avoids a startup double-marker).
    if (trackedAgentId.current != null && key === `agent-${trackedAgentId.current}`) return;
    const existing = markers.current[key];
    if (existing) {
      existing.setLatLng([lat, lng]);
      existing.setTooltipContent(label);
    } else {
      const m = L.circleMarker([lat, lng], { radius: 9, color: '#fff', weight: 2, fillColor: color, fillOpacity: 1 })
        .addTo(map).bindTooltip(label, { permanent: false, direction: 'top' });
      markers.current[key] = m;
    }
  }

  async function drawRoute(caseId: number) {
    const map = mapRef.current;
    const layer = routeLayer.current;
    if (!map || !layer) return;

    let route: RouteDto;
    try {
      const { data } = await api.get<RouteDto>(`/live/route/${caseId}`);
      route = data;
    } catch { return; }
    setRouteInfo(route);

    layer.clearLayers();
    driverMarker.current = null;
    trackedAgentId.current = route.driver?.agentId ?? null;

    // The tracked driver is drawn as the ambulance pin — drop its generic vehicle dot to avoid a double marker.
    if (trackedAgentId.current != null) {
      const key = `agent-${trackedAgentId.current}`;
      const generic = markers.current[key];
      if (generic) { generic.remove(); delete markers.current[key]; }
    }

    const pts: L.LatLngExpression[] = [];

    if (route.driver?.lat != null && route.driver?.lng != null) {
      driverMarker.current = L.marker([route.driver.lat, route.driver.lng], { icon: pin(DRIVER_COLOR, '🚑'), zIndexOffset: 1000 })
        .bindTooltip(`${route.driver.name} · ${route.driver.status}`, { direction: 'top' }).addTo(layer);
      pts.push([route.driver.lat, route.driver.lng]);
    }
    if (route.pickup) {
      L.marker([route.pickup.lat, route.pickup.lng], { icon: pin(PICKUP_COLOR, '📍') })
        .bindTooltip(`Pickup · ${route.pickup.label}`, { direction: 'top' }).addTo(layer);
      pts.push([route.pickup.lat, route.pickup.lng]);
    }
    if (route.destination) {
      L.marker([route.destination.lat, route.destination.lng], { icon: pin(HOSPITAL_COLOR, '🏥') })
        .bindTooltip(`Hospital · ${route.destination.label}`, { direction: 'top' }).addTo(layer);
      pts.push([route.destination.lat, route.destination.lng]);
    }

    if (pts.length >= 2) {
      map.fitBounds(L.latLngBounds(pts as L.LatLngTuple[]).pad(0.25));
      drawRoadPolyline(pts, layer);
    }
  }

  // Road-snapped line via the public OSRM routing service; falls back to straight segments offline.
  async function drawRoadPolyline(pts: L.LatLngExpression[], layer: L.LayerGroup) {
    const coords = (pts as L.LatLngTuple[]).map(([lat, lng]) => `${lng},${lat}`).join(';');
    let latlngs: L.LatLngExpression[] = pts;
    try {
      const res = await fetch(`https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`);
      const json = await res.json();
      const geo = json?.routes?.[0]?.geometry?.coordinates;
      if (Array.isArray(geo)) latlngs = geo.map((c: [number, number]) => [c[1], c[0]] as L.LatLngExpression);
    } catch { /* fall back to straight polyline */ }
    L.polyline(latlngs, { color: DRIVER_COLOR, weight: 5, opacity: 0.75 }).addTo(layer);
  }

  useLive((name, data) => {
    if (name === 'hello') { setConnected(true); return; }

    if (name === 'location' && data?.lat != null) {
      const isTrackedDriver = data.kind === 'agent' && Number(data.id) === trackedAgentId.current;
      // Move the tracked ambulance along the route; other vehicles show as generic dots.
      if (isTrackedDriver && driverMarker.current) {
        driverMarker.current.setLatLng([data.lat, data.lng]);
        driverMarker.current.setTooltipContent(`${data.name} · ${data.status}`);
      } else if (!isTrackedDriver) {
        upsertMarker(`${data.kind}-${data.id}`, data.lat, data.lng, `${data.name} · ${data.status}`, data.kind === 'ambulance' ? HOSPITAL_COLOR : DRIVER_COLOR);
      }
    }
    // A resource event may change an assignment's status — refresh the journey list.
    if (name === 'event') loadJourneys();

    const item: FeedItem = { id: ++feedSeq, name, at: Date.now(), text: '', sub: '' };
    if (name === 'event') { item.text = `${(data.type || '').replace(/_/g, ' ')}`; item.sub = `${data.description || ''} · case ${data.caseId}`; item.emergency = data.emergency; }
    else if (name === 'location') { item.text = `GPS · ${data.name}`; item.sub = `${data.status} (${Number(data.lat).toFixed(4)}, ${Number(data.lng).toFixed(4)})`; }
    else if (name === 'notification') { item.text = `${data.channel} · ${data.category}`; item.sub = `${data.subject} → ${data.recipient} (${data.status})`; }
    else if (name === 'request') { item.text = `New request #${data.id}`; item.sub = `${data.patientName} · ${(data.services || []).join(', ')}`; item.emergency = data.emergency; }
    setFeed((f) => [item, ...f].slice(0, 60));
  });

  return (
    <div>
      <div className="page-head">
        <h2>Live Operations</h2>
        <span className="badge" style={{ background: connected ? 'var(--success-soft)' : 'var(--danger-soft)', color: connected ? 'var(--success)' : 'var(--danger)' }}>
          <span style={{ width: 8, height: 8, borderRadius: 50, background: connected ? 'var(--success)' : 'var(--danger)', display: 'inline-block', animation: connected ? 'pulse 1.4s infinite' : undefined }} />
          {connected ? 'LIVE' : 'connecting…'}
        </span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 16 }}>
        <div className="card" style={{ overflow: 'hidden' }}>
          <div className="card-pad" style={{ borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
            <h3>Live Tracking Map</h3>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span className="muted" style={{ fontSize: 12 }}>Journey</span>
              <select
                value={selectedCase ?? ''}
                onChange={(e) => setSelectedCase(e.target.value ? Number(e.target.value) : null)}
                style={{ padding: '5px 8px', borderRadius: 8, border: '1px solid var(--border)', fontSize: 13 }}
              >
                {journeys.length === 0 && <option value="">No active journeys</option>}
                {journeys.map((j) => (
                  <option key={j.assignmentId} value={j.caseId}>{j.caseNumber} · {j.patientName} · {j.status.replace(/_/g, ' ')}</option>
                ))}
              </select>
            </div>
          </div>
          {routeInfo && (
            <div style={{ display: 'flex', gap: 16, padding: '8px 14px', fontSize: 12, flexWrap: 'wrap', borderBottom: '1px solid var(--border)' }}>
              <span><span style={{ color: DRIVER_COLOR }}>●</span> Ambulance{routeInfo.driver ? ` · ${routeInfo.driver.name} (${routeInfo.driver.status})` : ' · unassigned'}</span>
              <span><span style={{ color: PICKUP_COLOR }}>●</span> Pickup · {routeInfo.pickup?.label ?? '—'}</span>
              <span><span style={{ color: HOSPITAL_COLOR }}>●</span> Hospital · {routeInfo.destination?.label ?? '—'}</span>
            </div>
          )}
          <div ref={mapEl} style={{ height: 440, width: '100%' }} />
        </div>

        <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
          <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Live Event Feed</h3></div>
          <div style={{ overflowY: 'auto', maxHeight: 480, padding: 8 }}>
            {feed.length === 0 ? <div className="spinner">Waiting for events… trigger an action anywhere in the app.</div>
              : feed.map((f) => (
                <div key={f.id} style={{ display: 'flex', gap: 10, padding: '9px 10px', borderBottom: '1px solid var(--border)' }}>
                  <div style={{ width: 8, height: 8, borderRadius: 50, marginTop: 5, background: f.emergency ? 'var(--danger)' : EVENT_COLOR[f.name] }} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, fontSize: 13 }}>{f.text} {f.emergency && <span className="badge badge-emergency" style={{ fontSize: 9 }}>EMERGENCY</span>}</div>
                    <div className="muted" style={{ fontSize: 11.5 }}>{f.sub}</div>
                  </div>
                  <div className="muted" style={{ fontSize: 10 }}>{new Date(f.at).toLocaleTimeString()}</div>
                </div>
              ))}
          </div>
        </div>
      </div>
    </div>
  );
}
