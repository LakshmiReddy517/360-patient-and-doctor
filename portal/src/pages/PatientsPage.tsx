import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import type { Page, PatientSummary } from '../types';

function age(dob?: string): string {
  if (!dob) return '—';
  const d = new Date(dob);
  const diff = Date.now() - d.getTime();
  return String(Math.floor(diff / (365.25 * 24 * 3600 * 1000))) + ' yrs';
}

export default function PatientsPage() {
  const [rows, setRows] = useState<PatientSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const load = useCallback(() => {
    setLoading(true);
    api
      .get<Page<PatientSummary>>('/patients', { params: { q: q || undefined, size: 50 } })
      .then((r) => { setRows(r.data.content); setTotal(r.data.totalElements); })
      .finally(() => setLoading(false));
  }, [q]);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <div className="page-head">
        <h2>Patients <span className="muted" style={{ fontSize: 14 }}>({total})</span></h2>
        <button className="btn btn-primary" onClick={() => navigate('/patients/new')}>+ Register Patient</button>
      </div>

      <div className="toolbar">
        <input className="input" style={{ maxWidth: 320 }} placeholder="Search name or mobile…"
               value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load()} />
        <button className="btn" onClick={load}>Search</button>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr><th>Name</th><th>Gender</th><th>Age</th><th>Mobile</th><th>City</th><th>Registered</th></tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="spinner">Loading…</td></tr>
              ) : rows.length === 0 ? (
                <tr><td colSpan={6} className="spinner">No patients found</td></tr>
              ) : rows.map((p) => (
                <tr key={p.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/patients/${p.id}`)}>
                  <td style={{ fontWeight: 600 }}>{p.fullName}</td>
                  <td className="muted">{p.gender || '—'}</td>
                  <td className="muted">{age(p.dateOfBirth)}</td>
                  <td className="muted">{p.mobile || '—'}</td>
                  <td className="muted">{p.city || '—'}</td>
                  <td className="muted">{new Date(p.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
