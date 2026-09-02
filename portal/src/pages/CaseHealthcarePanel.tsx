import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Appointment, AppointmentStatus, Doctor, Page } from '../types';

const APT_BADGE: Record<string, string> = {
  REQUESTED: 'badge-hold', SLOT_RESERVED: 'badge-open', CONFIRMED: 'badge-open', REMINDED: 'badge-open',
  ARRIVED: 'badge-progress', IN_CONSULTATION: 'badge-progress', PRESCRIBED: 'badge-progress',
  COMPLETED: 'badge-progress', FOLLOW_UP: 'badge-progress', CANCELLED: 'badge-closed', NO_SHOW: 'badge-closed',
};

export default function CaseHealthcarePanel({ caseId }: { caseId: number }) {
  const [appts, setAppts] = useState<Appointment[]>([]);
  const [showBook, setShowBook] = useState(false);
  const [error, setError] = useState('');
  const [consultFor, setConsultFor] = useState<Appointment | null>(null);

  const load = useCallback(() => {
    api.get<Appointment[]>('/appointments', { params: { caseId } }).then((r) => setAppts(r.data));
  }, [caseId]);
  useEffect(() => { load(); }, [load]);

  async function setStatus(id: number, status: AppointmentStatus) {
    setError('');
    try { await api.put(`/appointments/${id}/status`, { status }); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  return (
    <div>
      {error && <div className="error-text">{error}</div>}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <h3>Appointments</h3>
        <button className="btn btn-primary btn-sm" onClick={() => setShowBook(true)}>+ Book Appointment</button>
      </div>

      {appts.length === 0 ? <div className="card card-pad muted">No appointments booked.</div> : appts.map((a) => (
        <div className="card card-pad" key={a.id} style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <strong>{a.doctorName}</strong> <span className="muted">· {a.department} · {a.hospitalName}</span>
              <div className="muted" style={{ fontSize: 13 }}>{new Date(a.scheduledAt).toLocaleString()} {a.reason ? `· ${a.reason}` : ''}</div>
            </div>
            <span className={'badge ' + (APT_BADGE[a.status] || 'badge-normal')}>{a.status.replace(/_/g, ' ')}</span>
          </div>
          {a.prescription && <div style={{ marginTop: 8, fontSize: 13 }}><strong>Rx:</strong> {a.prescription} {a.followUpAt && <span className="muted">· follow-up {new Date(a.followUpAt).toLocaleDateString()}</span>}</div>}
          <div style={{ display: 'flex', gap: 6, marginTop: 10, flexWrap: 'wrap' }}>
            {a.status === 'REQUESTED' && <button className="btn btn-sm" onClick={() => setStatus(a.id, 'CONFIRMED')}>Confirm</button>}
            {a.status === 'CONFIRMED' && <button className="btn btn-sm" onClick={() => setStatus(a.id, 'ARRIVED')}>Mark arrived</button>}
            {(a.status === 'ARRIVED' || a.status === 'IN_CONSULTATION') && <button className="btn btn-sm btn-primary" onClick={() => setConsultFor(a)}>Record consultation</button>}
            {!['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(a.status) && <button className="btn btn-sm btn-danger" onClick={() => setStatus(a.id, 'CANCELLED')}>Cancel</button>}
          </div>
        </div>
      ))}

      {showBook && <BookModal caseId={caseId} onClose={() => setShowBook(false)} onBooked={() => { setShowBook(false); load(); }} />}
      {consultFor && <ConsultModal appt={consultFor} onClose={() => setConsultFor(null)} onSaved={() => { setConsultFor(null); load(); }} />}
    </div>
  );
}

const overlay: React.CSSProperties = { position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 };

function BookModal({ caseId, onClose, onBooked }: { caseId: number; onClose: () => void; onBooked: () => void }) {
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [doctorId, setDoctorId] = useState<number | ''>('');
  const [scheduledAt, setScheduledAt] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => { api.get<Page<Doctor>>('/doctors', { params: { size: 50 } }).then((r) => setDoctors(r.data.content)); }, []);

  async function save() {
    if (!doctorId || !scheduledAt) { setError('Pick a doctor and time'); return; }
    setSaving(true); setError('');
    try { await api.post('/appointments', { caseId, doctorId, scheduledAt, reason }); onBooked(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 460, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>Book Appointment</h3>
        {error && <div className="error-text">{error}</div>}
        <div className="field"><label>Doctor</label>
          <select className="input" value={doctorId} onChange={(e) => setDoctorId(Number(e.target.value))}>
            <option value="">Select a doctor…</option>
            {doctors.map((d) => <option key={d.id} value={d.id}>{d.fullName} — {d.specialty} (₹{d.consultationFee})</option>)}
          </select>
        </div>
        <div className="field"><label>Date & time</label><input className="input" type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} /></div>
        <div className="field"><label>Reason</label><input className="input" value={reason} onChange={(e) => setReason(e.target.value)} /></div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Booking…' : 'Book'}</button>
        </div>
      </div>
    </div>
  );
}

function ConsultModal({ appt, onClose, onSaved }: { appt: Appointment; onClose: () => void; onSaved: () => void }) {
  const [consultationNotes, setNotes] = useState('');
  const [prescription, setRx] = useState('');
  const [followUpAt, setFollowUp] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function save() {
    setSaving(true); setError('');
    try {
      await api.post(`/appointments/${appt.id}/consultation`, { consultationNotes, prescription, followUpAt: followUpAt || null });
      onSaved();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 480, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 4 }}>Consultation — {appt.doctorName}</h3>
        <div style={{ background: 'var(--warning-soft)', color: 'var(--warning)', padding: '6px 10px', borderRadius: 6, fontSize: 12, marginBottom: 14 }}>
          Recorded by the authorised doctor. The platform stores; it does not diagnose or prescribe.
        </div>
        {error && <div className="error-text">{error}</div>}
        <div className="field"><label>Consultation notes</label><textarea className="input" rows={3} value={consultationNotes} onChange={(e) => setNotes(e.target.value)} /></div>
        <div className="field"><label>Prescription</label><textarea className="input" rows={2} value={prescription} onChange={(e) => setRx(e.target.value)} /></div>
        <div className="field"><label>Follow-up (optional)</label><input className="input" type="datetime-local" value={followUpAt} onChange={(e) => setFollowUp(e.target.value)} /></div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </div>
  );
}
