import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, apiErrorMessage } from '../api/client';
import { useLive } from '../api/live';
import type { CasePriority, CaseStatus, CaseSummary, Page } from '../types';
import { PriorityBadge, StatusBadge } from '../components/badges';

const STATUSES: CaseStatus[] = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED', 'CANCELLED'];

export default function CasesPage() {
  const [cases, setCases] = useState<CaseSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const navigate = useNavigate();

  const load = useCallback(() => {
    setLoading(true);
    api
      .get<Page<CaseSummary>>('/cases', { params: { q: q || undefined, status: status || undefined, size: 50 } })
      .then((r) => { setCases(r.data.content); setTotal(r.data.totalElements); })
      .finally(() => setLoading(false));
  }, [q, status]);

  useEffect(() => { load(); }, [load]);

  // Dynamic: cases update live as events stream in.
  useLive((name) => { if (name === 'event') load(); });

  return (
    <div>
      <div className="page-head">
        <h2>Cases <span className="muted" style={{ fontSize: 14 }}>({total})</span></h2>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ New Case</button>
      </div>

      <div className="toolbar">
        <input className="input" style={{ maxWidth: 320 }} placeholder="Search case #, patient or title…"
               value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load()} />
        <select className="input" style={{ maxWidth: 200 }} value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
        </select>
        <button className="btn" onClick={load}>Search</button>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr><th>Case #</th><th>Patient</th><th>Mobile</th><th>Title</th><th>Priority</th><th>Status</th><th>Owner</th></tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={7} className="spinner">Loading…</td></tr>
              ) : cases.length === 0 ? (
                <tr><td colSpan={7} className="spinner">No cases found</td></tr>
              ) : cases.map((c) => (
                <tr key={c.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/cases/${c.id}`)}>
                  <td className="mono">{c.caseNumber}</td>
                  <td>{c.patientName}</td>
                  <td className="muted">{c.patientMobile || '—'}</td>
                  <td className="muted">{c.title || '—'}</td>
                  <td><PriorityBadge priority={c.priority} emergency={c.emergency} /></td>
                  <td><StatusBadge status={c.status} /></td>
                  <td className="muted">{c.assignedToName || 'Unassigned'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showCreate && <CreateCaseModal onClose={() => setShowCreate(false)} onCreated={() => { setShowCreate(false); load(); }} />}
    </div>
  );
}

function CreateCaseModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [patientName, setPatientName] = useState('');
  const [patientMobile, setPatientMobile] = useState('');
  const [title, setTitle] = useState('');
  const [summary, setSummary] = useState('');
  const [priority, setPriority] = useState<CasePriority>('NORMAL');
  const [emergency, setEmergency] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function save() {
    setError(''); setSaving(true);
    try {
      await api.post('/cases', { patientName, patientMobile, title, summary, priority, emergency });
      onCreated();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 460, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>New Case</h3>
        {error && <div className="error-text">{error}</div>}
        <div className="field"><label>Patient name *</label><input className="input" value={patientName} onChange={(e) => setPatientName(e.target.value)} /></div>
        <div className="field"><label>Mobile</label><input className="input" value={patientMobile} onChange={(e) => setPatientMobile(e.target.value)} /></div>
        <div className="field"><label>Title</label><input className="input" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Airport pickup + hospital admission" /></div>
        <div className="field"><label>Summary</label><textarea className="input" rows={3} value={summary} onChange={(e) => setSummary(e.target.value)} /></div>
        <div className="field"><label>Priority</label>
          <select className="input" value={priority} onChange={(e) => setPriority(e.target.value as CasePriority)}>
            <option>LOW</option><option>NORMAL</option><option>HIGH</option><option>EMERGENCY</option>
          </select>
        </div>
        <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 13, marginBottom: 18 }}>
          <input type="checkbox" checked={emergency} onChange={(e) => setEmergency(e.target.checked)} /> Flag as emergency
        </label>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving || !patientName}>{saving ? 'Saving…' : 'Create Case'}</button>
        </div>
      </div>
    </div>
  );
}

const overlay: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50,
};
