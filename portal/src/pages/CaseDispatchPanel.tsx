import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import { useLive } from '../api/live';
import type { Agent, Ambulance, Assignment, AssignmentStatus, Trip } from '../types';

/** Straight-line distance (km) between two coordinates. */
function haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371, dLat = ((lat2 - lat1) * Math.PI) / 180, dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
/** Rough ETA at ~30 km/h city speed. */
function etaMinutes(km: number): number {
  return Math.max(1, Math.round((km / 30) * 60));
}

const ASSIGN_BADGE: Record<string, string> = {
  OFFERED: 'badge-hold', ACCEPTED: 'badge-open', EN_ROUTE: 'badge-open',
  ARRIVED_AT_PICKUP: 'badge-open', PATIENT_PICKED: 'badge-progress', IN_TRANSIT: 'badge-progress',
  HOSPITAL_ARRIVED: 'badge-progress', COMPLETED: 'badge-progress', CANCELLED: 'badge-closed',
};

const NEXT: { status: AssignmentStatus; label: string }[] = [
  { status: 'EN_ROUTE', label: 'En route' },
  { status: 'ARRIVED_AT_PICKUP', label: 'Arrived at pickup' },
  { status: 'IN_TRANSIT', label: 'In transit' },
  { status: 'HOSPITAL_ARRIVED', label: 'Hospital arrived' },
];

export default function CaseDispatchPanel({ caseId }: { caseId: number }) {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [available, setAvailable] = useState<Agent[]>([]);
  const [fleet, setFleet] = useState<Ambulance[]>([]);
  const [pickup, setPickup] = useState<{ lat: number; lng: number } | null>(null);
  const [error, setError] = useState('');
  const [otp, setOtp] = useState('');

  const load = useCallback(() => {
    Promise.all([
      api.get<Assignment[]>('/assignments', { params: { caseId } }),
      api.get<Agent[]>('/agents/available'),
      api.get<Ambulance[]>('/ambulances'),
      api.get<Trip>(`/cases/${caseId}/trip`).catch(() => null),
    ]).then(([asg, ag, amb, trip]) => {
      setAssignments(asg.data); setAvailable(ag.data); setFleet(amb.data);
      const p = trip?.data?.pickup;
      setPickup(p && p.latitude != null && p.longitude != null ? { lat: p.latitude, lng: p.longitude } : null);
    });
  }, [caseId]);
  useEffect(() => { load(); }, [load]);

  // Rank available agents by distance to the case pickup (blueprint point 64).
  const rankedAgents = available
    .map((a) => {
      const km = pickup && a.currentLatitude != null && a.currentLongitude != null
        ? haversineKm(pickup.lat, pickup.lng, a.currentLatitude, a.currentLongitude) : null;
      return { agent: a, km };
    })
    .sort((x, y) => (x.km ?? 1e9) - (y.km ?? 1e9));

  // Dynamic: refresh dispatch state live when agents/ambulances move or the case changes.
  useLive((name, data) => {
    if (name === 'location') { load(); return; }
    if (name === 'event' && String(data?.caseId) === String(caseId)) load();
  });

  async function assignAmbulance(ambulanceId: number) {
    setError('');
    try { await api.post('/ambulances/assign', { ambulanceId, caseId }); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  const dispatchableAmbulances = fleet.filter((a) => a.status === 'AVAILABLE' && a.ready);
  const caseAmbulance = fleet.find((a) => a.currentCaseId === caseId);

  async function assign(agentId: number) {
    setError('');
    try { await api.post('/assignments', { agentId, caseId }); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }
  async function act(id: number, path: string, body?: unknown) {
    setError('');
    try { await api.post(`/assignments/${id}/${path}`, body); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }
  async function setStatus(id: number, status: AssignmentStatus) {
    setError('');
    try { await api.put(`/assignments/${id}/status`, { status }); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  const active = assignments.find((a) => a.status !== 'COMPLETED' && a.status !== 'CANCELLED');

  return (
    <div>
      {error && <div className="error-text">{error}</div>}

      {!active && (
        <div className="card card-pad" style={{ marginBottom: 16 }}>
          <h3 style={{ marginBottom: 12 }}>Dispatch — available agents</h3>
          {!pickup && <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>Pickup coordinates unavailable — distance/ETA not shown. Nearest agent is auto-picked on approval.</div>}
          {available.length === 0 ? <div className="muted">No available agents.</div> : (
            <div className="table-wrap">
              <table className="data">
                <thead><tr><th>Agent</th><th>Distance</th><th>ETA</th><th>Skills</th><th>Languages</th><th>Workload</th><th></th></tr></thead>
                <tbody>
                  {rankedAgents.map(({ agent: a, km }, i) => (
                    <tr key={a.id}>
                      <td style={{ fontWeight: 600 }}>{a.fullName}{i === 0 && km != null && <span className="badge badge-progress" style={{ marginLeft: 8, fontSize: 9 }}>NEAREST</span>}</td>
                      <td className="mono">{km != null ? km.toFixed(1) + ' km' : '—'}</td>
                      <td className="muted">{km != null ? '~' + etaMinutes(km) + ' min' : '—'}</td>
                      <td><div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>{a.skills.map((s) => <span key={s} className="badge badge-normal" style={{ fontSize: 10 }}>{s}</span>)}</div></td>
                      <td className="muted" style={{ fontSize: 12 }}>{a.languages.join(', ')}</td>
                      <td className="muted">{a.activeAssignments} active</td>
                      <td><button className="btn btn-sm btn-primary" onClick={() => assign(a.id)}>Assign</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {active && (
        <div className="card card-pad" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>Active assignment #{active.id} — {active.agentName}</h3>
            <span className={'badge ' + (ASSIGN_BADGE[active.status] || 'badge-open')}>{active.status.replace(/_/g, ' ')}</span>
          </div>
          <div style={{ display: 'flex', gap: 20, margin: '12px 0', flexWrap: 'wrap' }}>
            <div className="card card-pad" style={{ background: 'var(--surface-2)', textAlign: 'center', minWidth: 130 }}>
              <div className="muted" style={{ fontSize: 11 }}>PICKUP OTP</div>
              <div className="mono" style={{ fontSize: 26, fontWeight: 700, letterSpacing: 3 }}>{active.pickupOtp}</div>
            </div>
            <div className="card card-pad" style={{ background: 'var(--surface-2)', textAlign: 'center', minWidth: 130 }}>
              <div className="muted" style={{ fontSize: 11 }}>HANDOVER OTP</div>
              <div className="mono" style={{ fontSize: 26, fontWeight: 700, letterSpacing: 3 }}>{active.handoverOtp}</div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 12 }}>
            {active.status === 'OFFERED' && <button className="btn btn-sm btn-primary" onClick={() => act(active.id, 'accept')}>Accept (agent)</button>}
            {NEXT.map((n) => <button key={n.status} className="btn btn-sm" onClick={() => setStatus(active.id, n.status)}>{n.label}</button>)}
            <button className="btn btn-sm btn-danger" onClick={() => act(active.id, 'cancel', { status: 'CANCELLED', note: 'Cancelled from console' })}>Cancel</button>
          </div>

          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <input className="input" style={{ maxWidth: 140 }} placeholder="Enter OTP" value={otp} onChange={(e) => setOtp(e.target.value)} />
            <button className="btn btn-sm" onClick={() => { act(active.id, 'verify-pickup', { otp }); setOtp(''); }}>Verify Pickup</button>
            <button className="btn btn-sm" onClick={() => { act(active.id, 'verify-handover', { otp }); setOtp(''); }}>Verify Handover</button>
          </div>
        </div>
      )}

      <div className="card card-pad" style={{ marginBottom: 16 }}>
        <h3 style={{ marginBottom: 10 }}>Ambulance dispatch</h3>
        {caseAmbulance ? (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <strong>{caseAmbulance.registrationNo}</strong> <span className="badge badge-normal">{caseAmbulance.category.replace(/_/g, ' ')}</span>
              <div className="muted" style={{ fontSize: 12 }}>Crew: {caseAmbulance.driverName || '—'} · O₂ {caseAmbulance.oxygenLevelPercent}%</div>
            </div>
            <span className="badge badge-progress">{caseAmbulance.status.replace(/_/g, ' ')}</span>
          </div>
        ) : dispatchableAmbulances.length === 0 ? (
          <div className="muted">No readiness-checked ambulances available.</div>
        ) : (
          <div className="table-wrap">
            <table className="data">
              <thead><tr><th>Vehicle</th><th>Category</th><th>O₂</th><th>Crew</th><th></th></tr></thead>
              <tbody>
                {dispatchableAmbulances.map((a) => (
                  <tr key={a.id}>
                    <td className="mono">{a.registrationNo}</td>
                    <td><span className="badge badge-normal">{a.category.replace(/_/g, ' ')}</span></td>
                    <td className="mono">{a.oxygenLevelPercent}%</td>
                    <td className="muted">{a.driverName || '—'}</td>
                    <td><button className="btn btn-sm btn-primary" onClick={() => assignAmbulance(a.id)}>Dispatch</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <h3 style={{ margin: '16px 0 10px' }}>Assignment history</h3>
      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>#</th><th>Agent</th><th>Status</th><th>Picked</th><th>Completed</th></tr></thead>
            <tbody>
              {assignments.length === 0 ? <tr><td colSpan={5} className="spinner">No assignments</td></tr> : assignments.map((a) => (
                <tr key={a.id}>
                  <td className="mono">#{a.id}</td>
                  <td>{a.agentName}</td>
                  <td><span className={'badge ' + (ASSIGN_BADGE[a.status] || 'badge-open')}>{a.status.replace(/_/g, ' ')}</span></td>
                  <td className="muted">{a.pickedAt ? new Date(a.pickedAt).toLocaleTimeString() : '—'}</td>
                  <td className="muted">{a.completedAt ? new Date(a.completedAt).toLocaleTimeString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
