import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Caretaker, Page } from '../types';

export default function CaretakersPage() {
  const [rows, setRows] = useState<Caretaker[]>([]);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);

  function load() {
    setLoading(true);
    api.get<Page<Caretaker>>('/caretakers', { params: { q: q || undefined, size: 50 } })
      .then((r) => setRows(r.data.content)).finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, []);

  return (
    <div>
      <div className="page-head"><h2>Caretakers</h2></div>
      <div className="toolbar">
        <input className="input" style={{ maxWidth: 300 }} placeholder="Search name…" value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load()} />
        <button className="btn" onClick={load}>Search</button>
      </div>
      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Name</th><th>Skills</th><th>Languages</th><th>Shift</th><th>Rate/hr</th><th>Verified</th><th>Available</th></tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={7} className="spinner">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan={7} className="spinner">No caretakers</td></tr>
                : rows.map((c) => (
                  <tr key={c.id}>
                    <td style={{ fontWeight: 600 }}>{c.fullName}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{c.mobile}</div></td>
                    <td><div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>{c.skills.map((s) => <span key={s} className="badge badge-normal" style={{ fontSize: 10 }}>{s}</span>)}</div></td>
                    <td className="muted" style={{ fontSize: 12 }}>{c.languages.join(', ')}</td>
                    <td className="muted">{c.shift || '—'}</td>
                    <td className="mono">₹{c.hourlyRate}</td>
                    <td>{c.verified ? <span className="badge badge-progress">✓</span> : <span className="badge badge-hold">pending</span>}</td>
                    <td>{c.available ? <span className="badge badge-progress">Yes</span> : <span className="badge badge-closed">No</span>}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
