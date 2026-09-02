import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Complaint, ComplaintStats, Page, Priority } from '../types';

const PRIORITY_BADGE: Record<string, string> = {
  URGENT: 'badge-emergency', HIGH: 'badge-high', MEDIUM: 'badge-open', LOW: 'badge-normal',
};
const STATUS_BADGE: Record<string, string> = {
  OPEN: 'badge-open', ASSIGNED: 'badge-open', INVESTIGATING: 'badge-hold',
  RESOLVED: 'badge-progress', CLOSED: 'badge-closed', REOPENED: 'badge-emergency',
};
const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export default function ComplaintsPage() {
  const [rows, setRows] = useState<Complaint[]>([]);
  const [stats, setStats] = useState<ComplaintStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      api.get<Page<Complaint>>('/complaints', { params: { size: 50 } }),
      api.get<ComplaintStats>('/complaints/stats'),
    ]).then(([c, s]) => { setRows(c.data.content); setStats(s.data); }).finally(() => setLoading(false));
  }, []);
  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <div className="page-head">
        <h2>Complaints</h2>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ Raise Complaint</button>
      </div>

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Open</div><div className="value primary">{stats?.open}</div></div>
        <div className="card stat"><div className="label">Investigating</div><div className="value">{stats?.investigating}</div></div>
        <div className="card stat"><div className="label">Resolved</div><div className="value">{stats?.resolved}</div></div>
        <div className="card stat"><div className="label">SLA Breached</div><div className="value danger">{stats?.breached}</div></div>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>#</th><th>Subject</th><th>Patient</th><th>Priority</th><th>Status</th><th>SLA</th><th>Case</th></tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={7} className="spinner">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan={7} className="spinner">No complaints</td></tr>
                : rows.map((c) => (
                  <tr key={c.id} style={{ cursor: 'pointer' }} onClick={() => setDetailId(c.id)}>
                    <td className="mono">#{c.id}</td>
                    <td style={{ fontWeight: 600 }}>{c.subject}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{c.category}</div></td>
                    <td className="muted">{c.patientName || '—'}</td>
                    <td><span className={'badge ' + (PRIORITY_BADGE[c.priority] || 'badge-normal')}>{c.priority}</span></td>
                    <td><span className={'badge ' + (STATUS_BADGE[c.status] || 'badge-normal')}>{c.status}</span></td>
                    <td>{c.resolutionBreached ? <span className="badge badge-emergency">BREACHED</span> : <span className="badge badge-progress">On time</span>}</td>
                    <td className="mono">{c.caseNumber || '—'}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {showCreate && <CreateModal priorities={PRIORITIES} onClose={() => setShowCreate(false)} onDone={() => { setShowCreate(false); load(); }} />}
      {detailId && <DetailModal id={detailId} onClose={() => setDetailId(null)} onChanged={load} />}
    </div>
  );
}

const overlay: React.CSSProperties = { position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 };

function CreateModal({ priorities, onClose, onDone }: { priorities: Priority[]; onClose: () => void; onDone: () => void }) {
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [patientName, setPatientName] = useState('');
  const [caseId, setCaseId] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function save() {
    setSaving(true); setError('');
    try {
      await api.post('/complaints', { subject, description, category, priority, patientName, caseId: caseId ? Number(caseId) : null });
      onDone();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 480, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>Raise Complaint</h3>
        {error && <div className="error-text">{error}</div>}
        <div className="field"><label>Subject *</label><input className="input" value={subject} onChange={(e) => setSubject(e.target.value)} /></div>
        <div className="field"><label>Description</label><textarea className="input" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} /></div>
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Category</label><input className="input" value={category} onChange={(e) => setCategory(e.target.value)} /></div>
          <div className="field" style={{ flex: 1 }}><label>Priority</label><select className="input" value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>{priorities.map((p) => <option key={p}>{p}</option>)}</select></div>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Patient name</label><input className="input" value={patientName} onChange={(e) => setPatientName(e.target.value)} /></div>
          <div className="field" style={{ flex: 1 }}><label>Case id (optional)</label><input className="input" value={caseId} onChange={(e) => setCaseId(e.target.value)} /></div>
        </div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving || !subject}>{saving ? 'Saving…' : 'Raise'}</button>
        </div>
      </div>
    </div>
  );
}

function DetailModal({ id, onClose, onChanged }: { id: number; onClose: () => void; onChanged: () => void }) {
  const [c, setC] = useState<Complaint | null>(null);
  const [comment, setComment] = useState('');
  const [resolution, setResolution] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => { api.get<Complaint>(`/complaints/${id}`).then((r) => setC(r.data)); }, [id]);
  useEffect(() => { load(); }, [load]);

  async function act(fn: () => Promise<unknown>) {
    setError('');
    try { await fn(); load(); onChanged(); } catch (e) { setError(apiErrorMessage(e)); }
  }

  if (!c) return null;

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 640, maxWidth: '95vw', maxHeight: '92vh', overflow: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h3>#{c.id} · {c.subject}</h3>
            <div className="muted" style={{ fontSize: 13 }}>{c.patientName} · {c.category} · {c.caseNumber || 'no case'}</div>
          </div>
          <div style={{ display: 'flex', gap: 6 }}>
            <span className={'badge ' + (PRIORITY_BADGE[c.priority] || 'badge-normal')}>{c.priority}</span>
            <span className={'badge ' + (STATUS_BADGE[c.status] || 'badge-normal')}>{c.status}</span>
          </div>
        </div>
        {error && <div className="error-text" style={{ marginTop: 10 }}>{error}</div>}

        <p style={{ marginTop: 12, lineHeight: 1.6 }}>{c.description}</p>

        <div style={{ display: 'flex', gap: 20, margin: '12px 0', fontSize: 12.5, flexWrap: 'wrap' }}>
          <span className="muted">Response due: {c.slaResponseDueAt ? new Date(c.slaResponseDueAt).toLocaleString() : '—'} {c.responseBreached && <span className="badge badge-emergency" style={{ fontSize: 10 }}>BREACHED</span>}</span>
          <span className="muted">Resolution due: {c.slaResolutionDueAt ? new Date(c.slaResolutionDueAt).toLocaleString() : '—'} {c.resolutionBreached && <span className="badge badge-emergency" style={{ fontSize: 10 }}>BREACHED</span>}</span>
          {c.reopenCount > 0 && <span className="muted">Reopened {c.reopenCount}×</span>}
        </div>

        <div className="toolbar">
          <button className="btn btn-sm" onClick={() => act(() => api.put(`/complaints/${id}/assign`, { userId: 1, name: 'Command Centre Admin' }))} disabled={c.status !== 'OPEN'}>Assign to me</button>
          <button className="btn btn-sm" onClick={() => act(() => api.put(`/complaints/${id}/status`, { status: 'INVESTIGATING' }))}>Investigate</button>
          <button className="btn btn-sm" onClick={() => act(() => api.put(`/complaints/${id}/status`, { status: 'REOPENED' }))}>Reopen</button>
          <button className="btn btn-sm" onClick={() => act(() => api.put(`/complaints/${id}/status`, { status: 'CLOSED' }))}>Close</button>
        </div>

        {c.status !== 'RESOLVED' && c.status !== 'CLOSED' && (
          <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
            <input className="input" placeholder="Resolution…" value={resolution} onChange={(e) => setResolution(e.target.value)} />
            <button className="btn btn-primary" onClick={() => act(() => api.put(`/complaints/${id}/resolve`, { resolution }))} disabled={!resolution.trim()}>Resolve</button>
          </div>
        )}
        {c.resolution && <div className="card card-pad" style={{ background: 'var(--success-soft)', marginBottom: 14 }}><strong>Resolution:</strong> {c.resolution}</div>}

        <h4 style={{ margin: '8px 0' }}>Communication history</h4>
        <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
          <input className="input" placeholder="Add a reply…" value={comment} onChange={(e) => setComment(e.target.value)} />
          <button className="btn btn-primary" onClick={() => act(() => api.post(`/complaints/${id}/comments`, { message: comment, internal: false }).then(() => setComment('')))} disabled={!comment.trim()}>Reply</button>
        </div>
        <div className="timeline">
          {c.comments.map((cm) => (
            <div className="tl-item" key={cm.id}>
              <div className="tl-dot" />
              <div className="tl-title" style={{ fontSize: 13 }}>{cm.message} {cm.internal && <span className="badge badge-normal" style={{ fontSize: 9 }}>internal</span>}</div>
              <div className="tl-meta">{new Date(cm.createdAt).toLocaleString()} · by {cm.author}</div>
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 14 }}><button className="btn" onClick={onClose}>Close</button></div>
      </div>
    </div>
  );
}
