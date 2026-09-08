import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type {
  Doctor,
  Page,
  Appointment,
  AppointmentStatus,
  Medicine,
  MedicationIntake,
  PatientConditionReading,
} from '../types';

// ---- Response shapes for the doctor dashboard + clinical endpoints ----
interface DoctorDashboard {
  doctorId: number;
  totalAppointments: number;
  upcoming: number;
  completed: number;
  pendingReview: number;
  followUps: number;
  next: Appointment[];
}

interface CaseClinical {
  caseId: number;
  appointments: Appointment[];
  vitals: PatientConditionReading[];
  medicines: Medicine[];
  medicationIntake: MedicationIntake[];
}

// Typed status → CSS badge class map.
const STATUS_BADGE: Record<AppointmentStatus, string> = {
  REQUESTED: 'badge-open',
  SLOT_RESERVED: 'badge-open',
  CONFIRMED: 'badge-open',
  REMINDED: 'badge-open',
  ARRIVED: 'badge-open',
  IN_CONSULTATION: 'badge-open',
  PRESCRIBED: 'badge-open',
  FOLLOW_UP: 'badge-open',
  COMPLETED: 'badge-progress',
  CANCELLED: 'badge-closed',
  NO_SHOW: 'badge-closed',
};

function StatusBadge({ status }: { status: AppointmentStatus }) {
  return <span className={'badge ' + (STATUS_BADGE[status] || 'badge-open')}>{status}</span>;
}

export default function DoctorsPage() {
  const [rows, setRows] = useState<Doctor[]>([]);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Doctor | null>(null);

  function load() {
    setLoading(true);
    api.get<Page<Doctor>>('/doctors', { params: { q: q || undefined, size: 50 } })
      .then((r) => setRows(r.data.content)).finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, []);

  return (
    <div>
      <div className="page-head"><h2>Doctors</h2></div>
      <div className="toolbar">
        <input className="input" style={{ maxWidth: 300 }} placeholder="Search name or specialty…" value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load()} />
        <button className="btn" onClick={load}>Search</button>
      </div>
      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Doctor</th><th>Specialty</th><th>Hospital</th><th>Qualification</th><th>Fee</th><th>Available</th><th></th></tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={7} className="spinner">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan={7} className="spinner">No doctors</td></tr>
                : rows.map((d) => (
                  <tr key={d.id} onClick={() => setSelected(d)} style={{ cursor: 'pointer', background: selected?.id === d.id ? 'var(--surface-2, rgba(99,102,241,.06))' : undefined }}>
                    <td style={{ fontWeight: 600 }}>{d.fullName}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{d.registrationNumber}</div></td>
                    <td>{d.specialty || '—'}</td>
                    <td className="muted">{d.hospitalName || '—'}</td>
                    <td className="muted" style={{ fontSize: 12 }}>{d.qualification || '—'}</td>
                    <td className="mono">₹{d.consultationFee}</td>
                    <td>{d.available ? <span className="badge badge-progress">Yes</span> : <span className="badge badge-closed">No</span>}</td>
                    <td><button className="btn btn-sm" onClick={(e) => { e.stopPropagation(); setSelected(d); }}>Open</button></td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {selected && (
        <div style={{ marginTop: 20 }}>
          <DoctorDetail doctor={selected} onClose={() => setSelected(null)} />
        </div>
      )}
    </div>
  );
}

function DoctorDetail({ doctor, onClose }: { doctor: Doctor; onClose: () => void }) {
  const [dash, setDash] = useState<DoctorDashboard | null>(null);
  const [appts, setAppts] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openCase, setOpenCase] = useState<{ caseId: number; label: string } | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    setOpenCase(null);
    Promise.all([
      api.get<DoctorDashboard>(`/doctors/${doctor.id}/dashboard`),
      api.get<Appointment[]>(`/doctors/${doctor.id}/appointments`),
    ]).then(([d, a]) => {
      setDash(d.data);
      setAppts(a.data);
    }).catch((e) => setError(apiErrorMessage(e))).finally(() => setLoading(false));
  }, [doctor.id]);
  useEffect(() => { load(); }, [load]);

  // Prefer dashboard.next for the upcoming list; fall back to the appointments feed.
  const upcoming = (dash?.next && dash.next.length > 0) ? dash.next : appts;

  return (
    <div className="card card-pad">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <div>
          <h3>{doctor.fullName}</h3>
          <div className="muted" style={{ fontSize: 12 }}>
            {[doctor.specialty, doctor.department, doctor.hospitalName].filter(Boolean).join(' · ') || '—'}
          </div>
        </div>
        <button className="btn btn-sm" onClick={onClose}>Close</button>
      </div>

      {error && <div className="error-text">{error}</div>}

      {loading ? <div className="spinner">Loading…</div> : (
        <>
          <div className="stat-grid" style={{ marginBottom: 20 }}>
            <div className="card stat"><div className="label">Total appointments</div><div className="value">{dash?.totalAppointments ?? 0}</div></div>
            <div className="card stat"><div className="label">Upcoming</div><div className="value primary">{dash?.upcoming ?? 0}</div></div>
            <div className="card stat"><div className="label">Completed</div><div className="value">{dash?.completed ?? 0}</div></div>
            <div className="card stat"><div className="label">Pending review</div><div className={'value' + ((dash?.pendingReview ?? 0) > 0 ? ' danger' : '')}>{dash?.pendingReview ?? 0}</div></div>
            <div className="card stat"><div className="label">Follow-ups</div><div className="value">{dash?.followUps ?? 0}</div></div>
          </div>

          <h4 style={{ marginBottom: 8, fontSize: 13 }}>Upcoming</h4>
          {upcoming.length === 0 ? <div className="muted" style={{ marginBottom: 8 }}>No upcoming appointments.</div> : (
            <div className="table-wrap">
              <table className="data">
                <thead><tr><th>Patient</th><th>Department</th><th>Scheduled</th><th>Status</th><th>Case</th></tr></thead>
                <tbody>
                  {upcoming.map((a) => (
                    <tr key={a.id}>
                      <td style={{ fontWeight: 600 }}>{a.patientName || '—'}</td>
                      <td className="muted">{a.department || a.hospitalName || '—'}</td>
                      <td className="mono">{a.scheduledAt ? new Date(a.scheduledAt).toLocaleString() : '—'}</td>
                      <td><StatusBadge status={a.status} /></td>
                      <td>
                        {a.caseId ? (
                          <button className="btn btn-sm" onClick={() => setOpenCase({ caseId: a.caseId!, label: a.caseNumber || `#${a.caseId}` })}>
                            {a.caseNumber || `Case #${a.caseId}`}
                          </button>
                        ) : <span className="muted">—</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {openCase && (
            <div style={{ marginTop: 20 }}>
              <CaseClinicalView caseId={openCase.caseId} label={openCase.label} onClose={() => setOpenCase(null)} />
            </div>
          )}
        </>
      )}
    </div>
  );
}

function CaseClinicalView({ caseId, label, onClose }: { caseId: number; label: string; onClose: () => void }) {
  const [data, setData] = useState<CaseClinical | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError('');
    api.get<CaseClinical>(`/doctors/cases/${caseId}/clinical`)
      .then((r) => { if (alive) setData(r.data); })
      .catch((e) => { if (alive) setError(apiErrorMessage(e)); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [caseId]);

  return (
    <div className="card card-pad">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <h4 style={{ fontSize: 14 }}>Clinical view · {label}</h4>
        <button className="btn btn-sm" onClick={onClose}>Close</button>
      </div>

      {error && <div className="error-text">{error}</div>}

      {loading ? <div className="spinner">Loading…</div> : (
        <>
          <h4 style={{ marginBottom: 8, fontSize: 13 }}>Appointments</h4>
          {(data?.appointments.length ?? 0) === 0 ? <div className="muted" style={{ marginBottom: 16 }}>No appointments.</div> : (
            <div className="table-wrap" style={{ marginBottom: 16 }}>
              <table className="data">
                <thead><tr><th>Doctor</th><th>Department</th><th>Scheduled</th><th>Status</th></tr></thead>
                <tbody>
                  {data!.appointments.map((a) => (
                    <tr key={a.id}>
                      <td>{a.doctorName || '—'}</td>
                      <td className="muted">{a.department || a.hospitalName || '—'}</td>
                      <td className="mono">{a.scheduledAt ? new Date(a.scheduledAt).toLocaleString() : '—'}</td>
                      <td><StatusBadge status={a.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <h4 style={{ marginBottom: 8, fontSize: 13 }}>Vitals</h4>
          {(data?.vitals.length ?? 0) === 0 ? <div className="muted" style={{ marginBottom: 16 }}>No vitals recorded.</div> : (
            <div className="table-wrap" style={{ marginBottom: 16 }}>
              <table className="data">
                <thead><tr><th>When</th><th>Condition</th><th>Temp</th><th>BP</th><th>Pulse</th><th>SpO₂</th><th>Pain</th><th>By</th></tr></thead>
                <tbody>
                  {data!.vitals.map((v) => (
                    <tr key={v.id}>
                      <td className="muted">{new Date(v.createdAt).toLocaleString()}</td>
                      <td>{v.condition || '—'}</td>
                      <td className="mono">{v.temperature || '—'}</td>
                      <td className="mono">{v.bloodPressure || '—'}</td>
                      <td className="mono">{v.pulse || '—'}</td>
                      <td className="mono" style={{ color: v.spo2 && parseInt(v.spo2) < 92 ? 'var(--danger)' : undefined }}>{v.spo2 || '—'}</td>
                      <td>{v.painScore ?? '—'}</td>
                      <td className="muted">{v.recordedBy || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <h4 style={{ marginBottom: 8, fontSize: 13 }}>Medicines</h4>
          {(data?.medicines.length ?? 0) === 0 ? <div className="muted" style={{ marginBottom: 16 }}>No medicines prescribed.</div> : (
            <div className="table-wrap" style={{ marginBottom: 16 }}>
              <table className="data">
                <thead><tr><th>Medicine</th><th>Dose</th><th>Frequency</th><th>Food</th><th>By</th></tr></thead>
                <tbody>
                  {data!.medicines.map((m) => (
                    <tr key={m.id}>
                      <td style={{ fontWeight: 600 }}>{m.name}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{[m.route, m.instructions].filter(Boolean).join(' · ')}</div></td>
                      <td>{m.dose || '—'}</td>
                      <td className="mono">{m.frequency || '—'}</td>
                      <td className="muted">{m.foodInstruction || '—'}</td>
                      <td className="muted">{m.prescribedBy || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <h4 style={{ marginBottom: 8, fontSize: 13 }}>Medication intake</h4>
          {(data?.medicationIntake.length ?? 0) === 0 ? <div className="muted">No intake logged.</div> : (
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {data!.medicationIntake.map((i) => (
                <span key={i.id} className={'badge ' + (i.status === 'TAKEN' ? 'badge-progress' : 'badge-closed')} title={new Date(i.createdAt).toLocaleString()}>
                  {i.status === 'TAKEN' ? '✓' : '✗'} {i.medicineName || `#${i.medicineId}`}
                </span>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
