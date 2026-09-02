import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, apiErrorMessage } from '../api/client';
import { useLive } from '../api/live';
import type { Page, RequestStats, RequestSummary, RequestStatus, ServiceType } from '../types';

const ALL_SERVICES: ServiceType[] = [
  'PICKUP_DROP', 'AMBULANCE', 'HOSPITAL_ADMISSION', 'DOCTOR_APPOINTMENT', 'CARETAKER',
  'ACCOMMODATION', 'FOOD', 'LOCAL_TRANSPORT', 'COMPLETE_CARE', 'FOLLOW_UP', 'OTHER',
];
const STATUSES: RequestStatus[] = ['NEW', 'UNDER_REVIEW', 'TRIAGE', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CLOSED', 'CANCELLED', 'EMERGENCY_ESCALATED'];

export function statusBadgeClass(s: RequestStatus): string {
  if (s === 'EMERGENCY_ESCALATED') return 'badge-emergency';
  if (['CANCELLED', 'REJECTED', 'EXPIRED', 'CLOSED'].includes(s)) return 'badge-closed';
  if (['CONFIRMED', 'IN_PROGRESS', 'COMPLETED'].includes(s)) return 'badge-progress';
  if (s === 'ON_HOLD') return 'badge-hold';
  return 'badge-open';
}

export default function RequestsPage() {
  const [rows, setRows] = useState<RequestSummary[]>([]);
  const [stats, setStats] = useState<RequestStats | null>(null);
  const [status, setStatus] = useState('');
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const navigate = useNavigate();

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      api.get<Page<RequestSummary>>('/requests', { params: { q: q || undefined, status: status || undefined, size: 50 } }),
      api.get<RequestStats>('/requests/stats'),
    ]).then(([r, s]) => { setRows(r.data.content); setStats(s.data); }).finally(() => setLoading(false));
  }, [q, status]);

  useEffect(() => { load(); }, [load]);

  // Dynamic: new requests booked from the Patient App appear here live.
  useLive((name) => { if (name === 'request') load(); });

  return (
    <div>
      <div className="page-head">
        <h2>Service Requests</h2>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ New Request</button>
      </div>

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">New</div><div className="value primary">{stats?.newRequests ?? '—'}</div></div>
        <div className="card stat"><div className="label">Under Review</div><div className="value">{stats?.underReview ?? '—'}</div></div>
        <div className="card stat"><div className="label">Triage</div><div className="value">{stats?.triage ?? '—'}</div></div>
        <div className="card stat"><div className="label">Confirmed</div><div className="value">{stats?.confirmed ?? '—'}</div></div>
        <div className="card stat"><div className="label">In Progress</div><div className="value">{stats?.inProgress ?? '—'}</div></div>
      </div>

      <div className="toolbar">
        <input className="input" style={{ maxWidth: 300 }} placeholder="Search patient or mobile…" value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load()} />
        <select className="input" style={{ maxWidth: 220 }} value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
        </select>
        <button className="btn" onClick={load}>Search</button>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>#</th><th>Patient</th><th>Services</th><th>Status</th><th>Case</th><th>Created</th></tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={6} className="spinner">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan={6} className="spinner">No requests</td></tr>
                : rows.map((r) => (
                  <tr key={r.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/requests/${r.id}`)}>
                    <td className="mono">#{r.id}</td>
                    <td style={{ fontWeight: 600 }}>{r.patientName}<div className="muted" style={{ fontWeight: 400, fontSize: 12 }}>{r.patientMobile}</div></td>
                    <td><div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>{r.services.map((s) => <span key={s} className="badge badge-normal" style={{ fontSize: 10 }}>{s.replace(/_/g, ' ')}</span>)}</div></td>
                    <td><span className={'badge ' + statusBadgeClass(r.status)}>{r.status.replace(/_/g, ' ')}</span></td>
                    <td className="mono">{r.caseNumber || '—'}</td>
                    <td className="muted">{new Date(r.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {showCreate && <CreateRequestModal services={ALL_SERVICES} onClose={() => setShowCreate(false)} onCreated={(id) => navigate(`/requests/${id}`)} />}
    </div>
  );
}

function CreateRequestModal({ services, onClose, onCreated }: { services: ServiceType[]; onClose: () => void; onCreated: (id: number) => void }) {
  const [patientName, setPatientName] = useState('');
  const [patientMobile, setPatientMobile] = useState('');
  const [selected, setSelected] = useState<ServiceType[]>([]);
  const [notes, setNotes] = useState('');
  const [emergency, setEmergency] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  function toggle(s: ServiceType) { setSelected((c) => (c.includes(s) ? c.filter((x) => x !== s) : [...c, s])); }

  async function save() {
    if (!patientName || selected.length === 0) { setError('Patient name and at least one service are required'); return; }
    setError(''); setSaving(true);
    try {
      const { data } = await api.post('/requests', { patientName, patientMobile, services: selected, notes, emergency });
      onCreated(data.id);
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 }} onClick={onClose}>
      <div className="card card-pad" style={{ width: 520, maxWidth: '92vw', maxHeight: '90vh', overflow: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>New Service Request</h3>
        {error && <div className="error-text">{error}</div>}
        <div style={{ display: 'flex', gap: 16 }}>
          <div className="field" style={{ flex: 1 }}><label>Patient name *</label><input className="input" value={patientName} onChange={(e) => setPatientName(e.target.value)} /></div>
          <div className="field" style={{ flex: 1 }}><label>Mobile</label><input className="input" value={patientMobile} onChange={(e) => setPatientMobile(e.target.value)} /></div>
        </div>
        <label style={{ fontSize: 12.5, fontWeight: 600 }}>Services required *</label>
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '8px 0 16px' }}>
          {services.map((s) => (
            <span key={s} onClick={() => toggle(s)} className={'badge ' + (selected.includes(s) ? 'badge-progress' : 'badge-normal')} style={{ cursor: 'pointer' }}>
              {s.replace(/_/g, ' ')}
            </span>
          ))}
        </div>
        <div className="field"><label>Notes</label><textarea className="input" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} /></div>
        <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 13, marginBottom: 18 }}>
          <input type="checkbox" checked={emergency} onChange={(e) => setEmergency(e.target.checked)} /> This is an emergency
        </label>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Create Request'}</button>
        </div>
      </div>
    </div>
  );
}
