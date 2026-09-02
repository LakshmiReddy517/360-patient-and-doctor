import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Doctor, Page } from '../types';

export default function DoctorsPage() {
  const [rows, setRows] = useState<Doctor[]>([]);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);

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
            <thead><tr><th>Doctor</th><th>Specialty</th><th>Hospital</th><th>Qualification</th><th>Fee</th><th>Available</th></tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={6} className="spinner">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan={6} className="spinner">No doctors</td></tr>
                : rows.map((d) => (
                  <tr key={d.id}>
                    <td style={{ fontWeight: 600 }}>{d.fullName}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{d.registrationNumber}</div></td>
                    <td>{d.specialty || '—'}</td>
                    <td className="muted">{d.hospitalName || '—'}</td>
                    <td className="muted" style={{ fontSize: 12 }}>{d.qualification || '—'}</td>
                    <td className="mono">₹{d.consultationFee}</td>
                    <td>{d.available ? <span className="badge badge-progress">Yes</span> : <span className="badge badge-closed">No</span>}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
