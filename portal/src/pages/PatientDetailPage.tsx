import { Fragment, useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api, apiErrorMessage } from '../api/client';
import type { Consent, DocumentAccess, Guardian, MedicalDocument, MedicalProfile, Patient } from '../types';

type Tab = 'profile' | 'medical' | 'guardians' | 'consent' | 'documents';

export default function PatientDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [tab, setTab] = useState<Tab>('profile');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<Patient>(`/patients/${id}`).then((r) => setPatient(r.data)).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="spinner">Loading patient…</div>;
  if (!patient) return <div className="spinner">Patient not found</div>;

  const tabs: { key: Tab; label: string }[] = [
    { key: 'profile', label: 'Profile' },
    { key: 'medical', label: 'Medical' },
    { key: 'guardians', label: 'Guardians' },
    { key: 'consent', label: 'Consent' },
    { key: 'documents', label: 'Documents' },
  ];

  return (
    <div>
      <div className="page-head">
        <div>
          <span className="link" onClick={() => navigate('/patients')}>← Patients</span>
          <h2 style={{ marginTop: 6 }}>{patient.fullName}</h2>
          <div className="muted">{patient.gender} · {patient.mobile || 'no mobile'} · {patient.city || '—'}</div>
        </div>
      </div>

      <div className="toolbar" style={{ gap: 4 }}>
        {tabs.map((t) => (
          <button key={t.key} className={'btn btn-sm' + (tab === t.key ? ' btn-primary' : '')} onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'profile' && <ProfileTab p={patient} />}
      {tab === 'medical' && <MedicalTab patientId={patient.id} />}
      {tab === 'guardians' && <GuardiansTab patientId={patient.id} />}
      {tab === 'consent' && <ConsentTab patientId={patient.id} />}
      {tab === 'documents' && <DocumentsTab patientId={patient.id} />}
    </div>
  );
}

function KV({ k, v }: { k: string; v?: string | number }) {
  return <div className="kv"><span className="k">{k}</span><span>{v || '—'}</span></div>;
}

function ProfileTab({ p }: { p: Patient }) {
  return (
    <div className="grid-2">
      <div className="card card-pad">
        <h3 style={{ marginBottom: 12 }}>Personal & Contact</h3>
        <KV k="Full name" v={p.fullName} />
        <KV k="Date of birth" v={p.dateOfBirth} />
        <KV k="Gender" v={p.gender} />
        <KV k="Mobile" v={p.mobile} />
        <KV k="Email" v={p.email} />
        <KV k="Address" v={[p.addressLine, p.city, p.state, p.country, p.pincode].filter(Boolean).join(', ')} />
        <KV k="Preferred language" v={p.preferredLanguage} />
      </div>
      <div>
        <div className="card card-pad">
          <h3 style={{ marginBottom: 12 }}>Emergency Contact</h3>
          <KV k="Name" v={p.emergencyContactName} />
          <KV k="Mobile" v={p.emergencyContactMobile} />
          <KV k="Relationship" v={p.emergencyContactRelation} />
        </div>
        <div className="card card-pad" style={{ marginTop: 20 }}>
          <h3 style={{ marginBottom: 12 }}>Identity & Insurance</h3>
          <KV k="ID type" v={p.identityType} />
          <KV k="ID number" v={p.identityNumber} />
          <KV k="Insurer" v={p.insuranceProvider} />
          <KV k="Policy no." v={p.insuranceNumber} />
        </div>
      </div>
    </div>
  );
}

function MedicalTab({ patientId }: { patientId: number }) {
  const [m, setM] = useState<MedicalProfile | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    api.get<MedicalProfile>(`/patients/${patientId}/medical`).then((r) => setM(r.data)).finally(() => setLoading(false));
  }, [patientId]);
  if (loading) return <div className="spinner">Loading…</div>;
  if (!m) return <div className="card card-pad muted">No medical profile recorded yet.</div>;
  return (
    <div className="card card-pad">
      <div style={{ background: 'var(--warning-soft)', color: 'var(--warning)', padding: '8px 12px', borderRadius: 8, fontSize: 12.5, marginBottom: 16 }}>
        ⚕ Information as provided by the patient. Clinical decisions remain with qualified clinicians.
      </div>
      <KV k="Blood group" v={m.bloodGroup} />
      <KV k="Current problem" v={m.currentProblem} />
      <KV k="Chronic diseases" v={m.chronicDiseases} />
      <KV k="Allergies" v={m.allergies} />
      <KV k="Current medicines" v={m.currentMedicines} />
      <KV k="Surgeries" v={m.surgeries} />
      <KV k="Previous hospitalisations" v={m.previousHospitalisations} />
      <KV k="Disability / mobility" v={m.disabilityOrMobility} />
      <KV k="Relevant history" v={m.relevantHistory} />
    </div>
  );
}

function GuardiansTab({ patientId }: { patientId: number }) {
  const [rows, setRows] = useState<Guardian[]>([]);
  const load = useCallback(() => { api.get<Guardian[]>(`/patients/${patientId}/guardians`).then((r) => setRows(r.data)); }, [patientId]);
  useEffect(() => { load(); }, [load]);
  return (
    <div className="card card-pad">
      <h3 style={{ marginBottom: 12 }}>Guardians & Family</h3>
      {rows.length === 0 ? <div className="muted">No guardians added.</div> : rows.map((g) => (
        <div key={g.id} style={{ padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
          <div style={{ fontWeight: 600 }}>{g.fullName} <span className="muted" style={{ fontWeight: 400 }}>· {g.relationship} · {g.mobile}</span></div>
          <div style={{ marginTop: 6, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            {g.permissions.map((p) => <span key={p} className="badge badge-open">{p.replace(/_/g, ' ')}</span>)}
          </div>
        </div>
      ))}
    </div>
  );
}

function ConsentTab({ patientId }: { patientId: number }) {
  const [rows, setRows] = useState<Consent[]>([]);
  const load = useCallback(() => { api.get<Consent[]>(`/patients/${patientId}/consents`).then((r) => setRows(r.data)); }, [patientId]);
  useEffect(() => { load(); }, [load]);
  async function revoke(cid: number) {
    await api.post(`/patients/${patientId}/consents/${cid}/revoke`);
    load();
  }
  return (
    <div className="card">
      <div className="table-wrap">
        <table className="data">
          <thead><tr><th>Scope</th><th>Granted to</th><th>Purpose</th><th>Valid</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {rows.length === 0 ? <tr><td colSpan={6} className="spinner">No consent records</td></tr> : rows.map((c) => (
              <tr key={c.id}>
                <td style={{ fontWeight: 600 }}>{c.scope}</td>
                <td>{c.grantedTo}</td>
                <td className="muted">{c.purpose || '—'}</td>
                <td className="muted">{c.validFrom || '—'} → {c.validTo || '—'}</td>
                <td><span className={'badge ' + (c.status === 'GRANTED' ? 'badge-progress' : 'badge-closed')}>{c.status}</span></td>
                <td>{c.status === 'GRANTED' && <button className="btn btn-sm btn-danger" onClick={() => revoke(c.id)}>Revoke</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function DocumentsTab({ patientId }: { patientId: number }) {
  const [rows, setRows] = useState<MedicalDocument[]>([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [logFor, setLogFor] = useState<number | null>(null);
  const [accessLog, setAccessLog] = useState<DocumentAccess[]>([]);
  const load = useCallback(() => { api.get<MedicalDocument[]>(`/patients/${patientId}/documents`).then((r) => setRows(r.data)); }, [patientId]);
  useEffect(() => { load(); }, [load]);

  async function toggleLog(d: MedicalDocument) {
    if (logFor === d.id) { setLogFor(null); return; }
    const r = await api.get<DocumentAccess[]>(`/patients/${patientId}/documents/${d.id}/access-log`);
    setAccessLog(r.data); setLogFor(d.id);
  }

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true); setError('');
    try {
      const form = new FormData();
      form.append('file', file);
      form.append('type', 'OTHER');
      form.append('title', file.name);
      await api.post(`/patients/${patientId}/documents`, form);
      load();
    } catch (err) { setError(apiErrorMessage(err)); } finally { setUploading(false); e.target.value = ''; }
  }

  async function download(d: MedicalDocument) {
    const res = await api.get(`/patients/${patientId}/documents/${d.id}/file`, { responseType: 'blob' });
    const url = URL.createObjectURL(res.data as Blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = d.originalFileName || 'document';
    a.click();
    URL.revokeObjectURL(url);
    if (logFor === d.id) toggleLog(d); // refresh the open log to show this access
  }

  return (
    <div className="card card-pad">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
        <h3>Medical Documents</h3>
        <label className="btn btn-primary">
          {uploading ? 'Uploading…' : '+ Upload'}
          <input type="file" hidden onChange={onFile} disabled={uploading} />
        </label>
      </div>
      {error && <div className="error-text">{error}</div>}
      {rows.length === 0 ? <div className="muted">No documents uploaded.</div> : (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Title</th><th>Type</th><th>Size</th><th>Uploaded by</th><th>When</th><th></th></tr></thead>
            <tbody>
              {rows.map((d) => (
                <Fragment key={d.id}>
                <tr>
                  <td style={{ fontWeight: 600 }}>{d.title || d.originalFileName || '—'}</td>
                  <td className="muted">{d.type.replace(/_/g, ' ')}</td>
                  <td className="muted">{d.sizeBytes ? Math.round(d.sizeBytes / 1024) + ' KB' : '—'}</td>
                  <td className="muted">{d.uploadedBy || 'system'}</td>
                  <td className="muted">{new Date(d.createdAt).toLocaleDateString()}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    {d.originalFileName && <span className="link" onClick={() => download(d)}>Download</span>}
                    <span className="link" style={{ marginLeft: 12 }} onClick={() => toggleLog(d)}>
                      {logFor === d.id ? 'Hide log' : 'Access log'}
                    </span>
                  </td>
                </tr>
                {logFor === d.id && (
                  <tr>
                    <td colSpan={6} style={{ background: 'var(--surface-2, #f8fafc)' }}>
                      {accessLog.length === 0 ? <span className="muted">No access recorded yet.</span> : (
                        <div style={{ fontSize: 12.5, lineHeight: 1.9 }}>
                          {accessLog.map((al) => (
                            <div key={al.id}>
                              <span className="badge badge-normal" style={{ fontSize: 10 }}>{al.action}</span>{' '}
                              by <strong>{al.accessedByName || 'unknown'}</strong>
                              {al.accessedByRole && <span className="muted"> ({al.accessedByRole})</span>}
                              {' · '}<span className="muted">{new Date(al.accessedAt).toLocaleString()}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </td>
                  </tr>
                )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
