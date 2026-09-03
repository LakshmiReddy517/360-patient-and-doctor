import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api, apiErrorMessage } from '../api/client';
import { useLive } from '../api/live';
import type { CaseDetail, CaseStatus } from '../types';
import { PriorityBadge, StatusBadge } from '../components/badges';
import CaseFinancePanel from './CaseFinancePanel';
import CaseDispatchPanel from './CaseDispatchPanel';
import CaseHealthcarePanel from './CaseHealthcarePanel';
import CaseCarePanel from './CaseCarePanel';
import CaseClinicalPanel from './CaseClinicalPanel';
import CaseServicePlanPanel from './CaseServicePlanPanel';
import CaseTripPanel from './CaseTripPanel';

const STATUSES: CaseStatus[] = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED', 'CANCELLED'];

export default function CaseDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<CaseDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [tab, setTab] = useState<'overview' | 'plan' | 'trip' | 'dispatch' | 'healthcare' | 'clinical' | 'care' | 'finance'>('overview');

  const load = useCallback(() => {
    setLoading(true);
    api.get<CaseDetail>(`/cases/${id}`).then((r) => setDetail(r.data)).finally(() => setLoading(false));
  }, [id]);

  // Silent refresh (no spinner) for live updates.
  const refresh = useCallback(() => {
    api.get<CaseDetail>(`/cases/${id}`).then((r) => setDetail(r.data)).catch(() => {});
  }, [id]);

  useEffect(() => { load(); }, [load]);

  // Dynamic: refresh this case's timeline live whenever an event for it streams in.
  useLive((name, data) => {
    if (name === 'event' && String(data?.caseId) === String(id)) refresh();
  });

  async function changeStatus(status: CaseStatus) {
    setBusy(true); setError('');
    try { await api.put(`/cases/${id}/status`, { status }); load(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setBusy(false); }
  }

  async function addNote() {
    if (!note.trim()) return;
    setBusy(true); setError('');
    try { await api.post(`/cases/${id}/notes`, { note }); setNote(''); load(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setBusy(false); }
  }

  if (loading) return <div className="spinner">Loading case…</div>;
  if (!detail) return <div className="spinner">Case not found</div>;

  const c = detail.caseFile;

  return (
    <div>
      <div className="page-head">
        <div>
          <span className="link" onClick={() => navigate('/cases')}>← Cases</span>
          <h2 style={{ marginTop: 6 }}>
            <span className="mono" style={{ fontSize: 20 }}>{c.caseNumber}</span> &nbsp;
            <StatusBadge status={c.status} /> &nbsp; <PriorityBadge priority={c.priority} emergency={c.emergency} />
          </h2>
        </div>
      </div>

      {error && <div className="error-text">{error}</div>}

      <div className="toolbar" style={{ gap: 4 }}>
        <button className={'btn btn-sm' + (tab === 'overview' ? ' btn-primary' : '')} onClick={() => setTab('overview')}>Overview</button>
        <button className={'btn btn-sm' + (tab === 'plan' ? ' btn-primary' : '')} onClick={() => setTab('plan')}>Service Plan</button>
        <button className={'btn btn-sm' + (tab === 'trip' ? ' btn-primary' : '')} onClick={() => setTab('trip')}>Trip</button>
        <button className={'btn btn-sm' + (tab === 'dispatch' ? ' btn-primary' : '')} onClick={() => setTab('dispatch')}>Dispatch</button>
        <button className={'btn btn-sm' + (tab === 'healthcare' ? ' btn-primary' : '')} onClick={() => setTab('healthcare')}>Healthcare</button>
        <button className={'btn btn-sm' + (tab === 'clinical' ? ' btn-primary' : '')} onClick={() => setTab('clinical')}>Clinical</button>
        <button className={'btn btn-sm' + (tab === 'care' ? ' btn-primary' : '')} onClick={() => setTab('care')}>Care Services</button>
        <button className={'btn btn-sm' + (tab === 'finance' ? ' btn-primary' : '')} onClick={() => setTab('finance')}>Finance</button>
      </div>

      {tab === 'plan' && <CaseServicePlanPanel caseId={c.id} />}
      {tab === 'trip' && <CaseTripPanel caseId={c.id} />}
      {tab === 'dispatch' && <CaseDispatchPanel caseId={c.id} />}
      {tab === 'healthcare' && <CaseHealthcarePanel caseId={c.id} />}
      {tab === 'clinical' && <CaseClinicalPanel caseId={c.id} />}
      {tab === 'care' && <CaseCarePanel caseId={c.id} />}
      {tab === 'finance' && <CaseFinancePanel caseId={c.id} />}

      {tab === 'overview' && (<>
      <div className="toolbar">
        <span className="muted">Change status:</span>
        {STATUSES.map((s) => (
          <button key={s} className={'btn btn-sm' + (s === c.status ? ' btn-primary' : '')} disabled={busy || s === c.status} onClick={() => changeStatus(s)}>
            {s.replace('_', ' ')}
          </button>
        ))}
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Case Timeline</h3></div>
          <div className="card-pad">
            <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
              <input className="input" placeholder="Add a note to the timeline…" value={note} onChange={(e) => setNote(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && addNote()} />
              <button className="btn btn-primary" onClick={addNote} disabled={busy || !note.trim()}>Add</button>
            </div>
            <div className="timeline">
              {detail.timeline.map((ev) => (
                <div className="tl-item" key={ev.id}>
                  <div className={'tl-dot' + (ev.type === 'EMERGENCY_ESCALATED' ? ' emergency' : '')} />
                  <div className="tl-title">{ev.description}</div>
                  <div className="tl-meta">
                    {ev.type.replace(/_/g, ' ')} · {new Date(ev.createdAt).toLocaleString()} · by {ev.createdBy || 'system'}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div>
          <div className="card card-pad">
            <h3 style={{ marginBottom: 12 }}>Patient</h3>
            <div className="kv"><span className="k">Name</span><span>{c.patientName}</span></div>
            <div className="kv"><span className="k">Mobile</span><span>{c.patientMobile || '—'}</span></div>
            <div className="kv"><span className="k">Case owner</span><span>{c.assignedToName || 'Unassigned'}</span></div>
            <div className="kv"><span className="k">Created</span><span>{new Date(c.createdAt).toLocaleString()}</span></div>
          </div>
          <div className="card card-pad" style={{ marginTop: 20 }}>
            <h3 style={{ marginBottom: 12 }}>Request</h3>
            <div className="kv"><span className="k">Title</span><span>{c.title || '—'}</span></div>
            <p className="muted" style={{ marginTop: 12, lineHeight: 1.6 }}>{detail.summary || 'No summary provided.'}</p>
          </div>
        </div>
      </div>
      </>)}
    </div>
  );
}
