import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api, apiErrorMessage } from '../api/client';
import type { RequestDetail, RequestStatus } from '../types';
import { statusBadgeClass } from './RequestsPage';

const FLOW: RequestStatus[] = ['UNDER_REVIEW', 'TRIAGE', 'QUOTE_PREPARED', 'QUOTE_SENT', 'ACCEPTED', 'PAYMENT_PENDING', 'CONFIRMED', 'RESOURCE_ASSIGNMENT', 'IN_PROGRESS', 'COMPLETED', 'CLOSED'];
const EXCEPTIONS: RequestStatus[] = ['ON_HOLD', 'CANCELLED', 'REJECTED', 'EMERGENCY_ESCALATED'];

export default function RequestDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [r, setR] = useState<RequestDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [triageNotes, setTriageNotes] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    api.get<RequestDetail>(`/requests/${id}`).then((res) => setR(res.data)).finally(() => setLoading(false));
  }, [id]);
  useEffect(() => { load(); }, [load]);

  async function transition(status: RequestStatus) {
    setBusy(true); setError('');
    try { await api.put(`/requests/${id}/status`, { status }); load(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setBusy(false); }
  }
  async function triage(emergency: boolean) {
    setBusy(true); setError('');
    try { await api.put(`/requests/${id}/triage`, { emergency, triageNotes }); load(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setBusy(false); }
  }
  async function convert() {
    setBusy(true); setError('');
    try { await api.post(`/requests/${id}/convert-to-case`); load(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setBusy(false); }
  }

  if (loading) return <div className="spinner">Loading…</div>;
  if (!r) return <div className="spinner">Request not found</div>;

  return (
    <div>
      <div className="page-head">
        <div>
          <span className="link" onClick={() => navigate('/requests')}>← Requests</span>
          <h2 style={{ marginTop: 6 }}>Request #{r.id} · {r.patientName}
            {' '}<span className={'badge ' + statusBadgeClass(r.status)}>{r.status.replace(/_/g, ' ')}</span>
            {r.emergency && <span className="badge badge-emergency" style={{ marginLeft: 6 }}>● EMERGENCY</span>}
          </h2>
        </div>
        {r.caseNumber ? (
          <button className="btn btn-primary" onClick={() => navigate(`/cases/${r.caseId}`)}>Open Case {r.caseNumber} →</button>
        ) : (
          <button className="btn btn-primary" onClick={convert} disabled={busy}>Convert to Case</button>
        )}
      </div>

      {error && <div className="error-text">{error}</div>}

      <div className="grid-2">
        <div>
          <div className="card card-pad">
            <h3 style={{ marginBottom: 12 }}>Requested services</h3>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {r.services.map((s) => <span key={s} className="badge badge-open">{s.replace(/_/g, ' ')}</span>)}
            </div>
            {r.notes && <p className="muted" style={{ marginTop: 14, lineHeight: 1.6 }}>{r.notes}</p>}
          </div>

          <div className="card card-pad" style={{ marginTop: 20 }}>
            <h3 style={{ marginBottom: 12 }}>Pickup</h3>
            <div className="kv"><span className="k">Source</span><span>{r.pickup.source?.replace(/_/g, ' ') || '—'}</span></div>
            <div className="kv"><span className="k">Address</span><span>{r.pickup.address || '—'}</span></div>
            <div className="kv"><span className="k">Scheduled</span><span>{r.pickup.scheduledAt ? new Date(r.pickup.scheduledAt).toLocaleString() : '—'}</span></div>
            <div className="kv"><span className="k">Flight/Train</span><span>{r.pickup.flightOrTrainNumber || '—'}</span></div>
          </div>

          <div className="card card-pad" style={{ marginTop: 20 }}>
            <h3 style={{ marginBottom: 12 }}>Destination</h3>
            <div className="kv"><span className="k">Hospital</span><span>{r.destination.hospitalName || '—'}</span></div>
            <div className="kv"><span className="k">Department</span><span>{r.destination.department || '—'}</span></div>
            <div className="kv"><span className="k">Doctor</span><span>{r.destination.doctorName || '—'}</span></div>
            <div className="kv"><span className="k">Appointment</span><span>{r.destination.appointmentAt ? new Date(r.destination.appointmentAt).toLocaleString() : '—'}</span></div>
          </div>
        </div>

        <div>
          <div className="card card-pad">
            <h3 style={{ marginBottom: 12 }}>Triage</h3>
            <div style={{ background: 'var(--danger-soft)', color: 'var(--danger)', padding: '8px 12px', borderRadius: 8, fontSize: 12, marginBottom: 12, lineHeight: 1.5 }}>
              For a real medical emergency, direct the caller to emergency services (112). This flag escalates coordination — it is not a substitute for emergency medical care.
            </div>
            <textarea className="input" rows={2} placeholder="Triage notes…" value={triageNotes} onChange={(e) => setTriageNotes(e.target.value)} />
            <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
              <button className="btn btn-danger" onClick={() => triage(true)} disabled={busy}>Flag Emergency</button>
              <button className="btn" onClick={() => triage(false)} disabled={busy}>Mark Non-emergency</button>
            </div>
            {r.triageNotes && <p className="muted" style={{ marginTop: 12 }}>Last triage: {r.triageNotes}</p>}
          </div>

          <div className="card card-pad" style={{ marginTop: 20 }}>
            <h3 style={{ marginBottom: 12 }}>Advance lifecycle</h3>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {FLOW.map((s) => <button key={s} className="btn btn-sm" disabled={busy || s === r.status} onClick={() => transition(s)}>{s.replace(/_/g, ' ')}</button>)}
            </div>
            <h4 style={{ margin: '16px 0 8px', fontSize: 13 }}>Exceptions</h4>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {EXCEPTIONS.map((s) => <button key={s} className="btn btn-sm" style={{ borderColor: 'var(--danger)', color: 'var(--danger)' }} disabled={busy || s === r.status} onClick={() => transition(s)}>{s.replace(/_/g, ' ')}</button>)}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
