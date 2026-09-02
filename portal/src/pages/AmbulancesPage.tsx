import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Ambulance, Emt, FleetStats } from '../types';

const STATUS_BADGE: Record<string, string> = {
  AVAILABLE: 'badge-progress', EN_ROUTE: 'badge-open', AT_PICKUP: 'badge-open', TRANSPORTING: 'badge-open',
  AT_HOSPITAL: 'badge-open', MAINTENANCE: 'badge-hold', OFFLINE: 'badge-closed', OUT_OF_SERVICE: 'badge-emergency',
};
const CHECK_ITEMS = [
  'fuelOk', 'engineOk', 'tyresOk', 'batteryOk', 'lightsOk', 'sirenOk',
  'gpsOk', 'stretcherOk', 'wheelchairOk', 'oxygenOk', 'suctionOk', 'firstAidPpeOk',
] as const;

export default function AmbulancesPage() {
  const [fleet, setFleet] = useState<Ambulance[]>([]);
  const [stats, setStats] = useState<FleetStats | null>(null);
  const [emts, setEmts] = useState<Emt[]>([]);
  const [checkFor, setCheckFor] = useState<Ambulance | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      api.get<Ambulance[]>('/ambulances'),
      api.get<FleetStats>('/ambulances/stats'),
      api.get<Emt[]>('/emts'),
    ]).then(([f, s, e]) => { setFleet(f.data); setStats(s.data); setEmts(e.data); }).finally(() => setLoading(false));
  }, []);
  useEffect(() => { load(); }, [load]);

  if (loading) return <div className="spinner">Loading fleet…</div>;

  const active = fleet.filter((a) => a.status !== 'OFFLINE');

  return (
    <div>
      <div className="page-head"><h2>Ambulance Fleet</h2></div>

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Total</div><div className="value">{stats?.total}</div></div>
        <div className="card stat"><div className="label">Available</div><div className="value primary">{stats?.available}</div></div>
        <div className="card stat"><div className="label">On Trip</div><div className="value">{stats?.onTrip}</div></div>
        <div className="card stat"><div className="label">Maintenance</div><div className="value">{stats?.maintenance}</div></div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Vehicle</th><th>Category</th><th>Status</th><th>Ready</th><th>O₂</th><th>Fuel</th><th>Crew</th><th>Current case</th><th></th></tr></thead>
            <tbody>
              {fleet.map((a) => (
                <tr key={a.id}>
                  <td style={{ fontWeight: 600 }}>{a.registrationNo}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{a.code}</div></td>
                  <td><span className="badge badge-normal">{a.category.replace(/_/g, ' ')}</span></td>
                  <td><span className={'badge ' + (STATUS_BADGE[a.status] || 'badge-normal')}>{a.status.replace(/_/g, ' ')}</span></td>
                  <td>{a.ready ? <span className="badge badge-progress">Ready</span> : <span className="badge badge-hold">Not ready</span>}</td>
                  <td className="mono" style={{ color: a.oxygenLevelPercent < 20 ? 'var(--danger)' : undefined }}>{a.oxygenLevelPercent}%</td>
                  <td className="mono">{a.fuelPercent}%</td>
                  <td className="muted">{a.driverName || '—'}</td>
                  <td className="mono">{a.currentCaseNumber || '—'}</td>
                  <td><button className="btn btn-sm" onClick={() => setCheckFor(a)}>Readiness</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <h3 style={{ margin: '8px 0 10px' }}>Live tracking</h3>
      <div className="card" style={{ marginBottom: 20 }}>
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Vehicle</th><th>Status</th><th>Location</th><th>Case</th><th>Patient</th><th>Updated</th></tr></thead>
            <tbody>
              {active.length === 0 ? <tr><td colSpan={6} className="spinner">No active ambulances</td></tr> : active.map((a) => (
                <tr key={a.id}>
                  <td className="mono">{a.registrationNo}</td>
                  <td><span className={'badge ' + (STATUS_BADGE[a.status] || 'badge-normal')}>{a.status.replace(/_/g, ' ')}</span></td>
                  <td className="muted">{a.currentLatitude != null ? `${a.currentLatitude.toFixed(4)}, ${a.currentLongitude?.toFixed(4)}` : 'no GPS'}</td>
                  <td className="mono">{a.currentCaseNumber || '—'}</td>
                  <td className="muted">{a.currentPatientName || '—'}</td>
                  <td className="muted">{a.locationUpdatedAt ? new Date(a.locationUpdatedAt).toLocaleTimeString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <h3 style={{ margin: '8px 0 10px' }}>Crew (Drivers / EMTs)</h3>
      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Name</th><th>Certification</th><th>Licence</th><th>Fitness</th><th>Verified</th><th>Shift</th></tr></thead>
            <tbody>
              {emts.map((e) => (
                <tr key={e.id}>
                  <td style={{ fontWeight: 600 }}>{e.fullName}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{e.mobile}</div></td>
                  <td>{e.certification || '—'}</td>
                  <td className="muted">{e.licenceNumber || '—'}</td>
                  <td>{e.medicalFitnessValid ? <span className="badge badge-progress">Valid</span> : <span className="badge badge-emergency">Invalid</span>}</td>
                  <td>{e.backgroundVerified ? <span className="badge badge-progress">✓</span> : <span className="badge badge-hold">pending</span>}</td>
                  <td className="muted">{e.shift || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {checkFor && <ReadinessModal ambulance={checkFor} items={CHECK_ITEMS} onClose={() => setCheckFor(null)} onDone={() => { setCheckFor(null); load(); }} />}
    </div>
  );
}

function ReadinessModal({ ambulance, items, onClose, onDone }:
  { ambulance: Ambulance; items: readonly string[]; onClose: () => void; onDone: () => void }) {
  const [checks, setChecks] = useState<Record<string, boolean>>(() => Object.fromEntries(items.map((i) => [i, true])));
  const [oxygen, setOxygen] = useState('95');
  const [fuel, setFuel] = useState('80');
  const [notes, setNotes] = useState('');
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function submit() {
    setSaving(true); setError(''); setResult(null);
    try {
      const body = { ...checks, reportedOxygenPercent: Number(oxygen), reportedFuelPercent: Number(fuel), notes };
      const { data } = await api.post(`/ambulances/${ambulance.id}/readiness`, body);
      setResult((data.passed ? '✓ ' : '✗ ') + data.message);
      if (data.passed) setTimeout(onDone, 900);
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 }} onClick={onClose}>
      <div className="card card-pad" style={{ width: 520, maxWidth: '92vw', maxHeight: '90vh', overflow: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 4 }}>Readiness — {ambulance.registrationNo}</h3>
        <div className="muted" style={{ fontSize: 12, marginBottom: 12 }}>Required items vary by category ({ambulance.category.replace(/_/g, ' ')}).</div>
        {error && <div className="error-text">{error}</div>}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6, marginBottom: 12 }}>
          {items.map((i) => (
            <label key={i} style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 13 }}>
              <input type="checkbox" checked={checks[i]} onChange={(e) => setChecks((c) => ({ ...c, [i]: e.target.checked }))} />
              {i.replace(/Ok$/, '').replace(/([A-Z])/g, ' $1')}
            </label>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Oxygen %</label><input className="input" type="number" value={oxygen} onChange={(e) => setOxygen(e.target.value)} /></div>
          <div className="field" style={{ flex: 1 }}><label>Fuel %</label><input className="input" type="number" value={fuel} onChange={(e) => setFuel(e.target.value)} /></div>
        </div>
        <div className="field"><label>Notes</label><input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} /></div>
        {result && <div style={{ fontWeight: 600, color: result.startsWith('✓') ? 'var(--success)' : 'var(--danger)', marginBottom: 10 }}>{result}</div>}
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Close</button>
          <button className="btn btn-primary" onClick={submit} disabled={saving}>{saving ? 'Submitting…' : 'Submit check'}</button>
        </div>
      </div>
    </div>
  );
}
