import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Incident, IncidentSeverity, IncidentStats, IncidentStatus, IncidentType, Page } from '../types';

const TYPES: IncidentType[] = ['PATIENT_UNAVAILABLE', 'WRONG_LOCATION', 'VEHICLE_BREAKDOWN', 'PATIENT_DETERIORATION',
  'ADMISSION_ISSUE', 'APPOINTMENT_CANCELLATION', 'PAYMENT_FAILURE', 'DOCUMENT_GAP', 'SERVICE_COMPLAINT', 'OTHER'];
const SEVERITIES: IncidentSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const STATUSES: IncidentStatus[] = ['OPEN', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'CLOSED'];

const SEV_BADGE: Record<string, string> = { CRITICAL: 'badge-emergency', HIGH: 'badge-high', MEDIUM: 'badge-open', LOW: 'badge-normal' };
const STATUS_BADGE: Record<string, string> = { OPEN: 'badge-open', IN_PROGRESS: 'badge-hold', ESCALATED: 'badge-emergency', RESOLVED: 'badge-progress', CLOSED: 'badge-closed' };

export default function IncidentsPage() {
  const [rows, setRows] = useState<Incident[]>([]);
  const [stats, setStats] = useState<IncidentStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      api.get<Page<Incident>>('/incidents', { params: { size: 50 } }),
      api.get<IncidentStats>('/incidents/stats'),
    ]).then(([i, s]) => { setRows(i.data.content); setStats(s.data); }).finally(() => setLoading(false));
  }, []);
  useEffect(() => { load(); }, [load]);

  async function setStatus(id: number, status: IncidentStatus) {
    setError('');
    try { await api.put(`/incidents/${id}/status`, { status }); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  return (
    <div>
      <div className="page-head">
        <h2>Incidents</h2>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ Report Incident</button>
      </div>
      {error && <div className="error-text">{error}</div>}

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Open</div><div className="value primary">{stats?.open}</div></div>
        <div className="card stat"><div className="label">In Progress</div><div className="value">{stats?.inProgress}</div></div>
        <div className="card stat"><div className="label">Escalated</div><div className="value danger">{stats?.escalated}</div></div>
        <div className="card stat"><div className="label">Resolved</div><div className="value">{stats?.resolved}</div></div>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>#</th><th>Type</th><th>Severity</th><th>Description</th><th>Status</th><th>Case</th><th>Actions</th></tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={7} className="spinner">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan={7} className="spinner">No incidents</td></tr>
                : rows.map((i) => (
                  <tr key={i.id}>
                    <td className="mono">#{i.id}</td>
                    <td>{i.type.replace(/_/g, ' ')}</td>
                    <td><span className={'badge ' + (SEV_BADGE[i.severity] || 'badge-normal')}>{i.severity}</span></td>
                    <td className="muted" style={{ fontSize: 12, maxWidth: 280 }}>{i.description}</td>
                    <td><span className={'badge ' + (STATUS_BADGE[i.status] || 'badge-normal')}>{i.status.replace(/_/g, ' ')}</span></td>
                    <td className="mono">{i.caseNumber || '—'}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        {i.status === 'OPEN' && <button className="btn btn-sm" onClick={() => setStatus(i.id, 'IN_PROGRESS')}>Start</button>}
                        {i.status !== 'ESCALATED' && i.status !== 'RESOLVED' && i.status !== 'CLOSED' && <button className="btn btn-sm btn-danger" onClick={() => setStatus(i.id, 'ESCALATED')}>Escalate</button>}
                        {i.status !== 'RESOLVED' && i.status !== 'CLOSED' && <button className="btn btn-sm" onClick={() => setStatus(i.id, 'RESOLVED')}>Resolve</button>}
                      </div>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {showCreate && <CreateModal types={TYPES} severities={SEVERITIES} statuses={STATUSES} onClose={() => setShowCreate(false)} onDone={() => { setShowCreate(false); load(); }} />}
    </div>
  );
}

function CreateModal({ types, severities, onClose, onDone }:
  { types: IncidentType[]; severities: IncidentSeverity[]; statuses: IncidentStatus[]; onClose: () => void; onDone: () => void }) {
  const [type, setType] = useState<IncidentType>('OTHER');
  const [severity, setSeverity] = useState<IncidentSeverity>('MEDIUM');
  const [description, setDescription] = useState('');
  const [caseId, setCaseId] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function save() {
    setSaving(true); setError('');
    try { await api.post('/incidents', { type, severity, description, caseId: caseId ? Number(caseId) : null }); onDone(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 }} onClick={onClose}>
      <div className="card card-pad" style={{ width: 460, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>Report Incident</h3>
        {error && <div className="error-text">{error}</div>}
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 2 }}><label>Type</label><select className="input" value={type} onChange={(e) => setType(e.target.value as IncidentType)}>{types.map((t) => <option key={t}>{t}</option>)}</select></div>
          <div className="field" style={{ flex: 1 }}><label>Severity</label><select className="input" value={severity} onChange={(e) => setSeverity(e.target.value as IncidentSeverity)}>{severities.map((s) => <option key={s}>{s}</option>)}</select></div>
        </div>
        <div className="field"><label>Description *</label><textarea className="input" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} /></div>
        <div className="field"><label>Case id (optional)</label><input className="input" value={caseId} onChange={(e) => setCaseId(e.target.value)} /></div>
        <div style={{ background: 'var(--danger-soft)', color: 'var(--danger)', padding: '6px 10px', borderRadius: 6, fontSize: 12, marginBottom: 12 }}>
          CRITICAL incidents auto-escalate to the case's emergency path.
        </div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving || !description}>{saving ? 'Reporting…' : 'Report'}</button>
        </div>
      </div>
    </div>
  );
}
