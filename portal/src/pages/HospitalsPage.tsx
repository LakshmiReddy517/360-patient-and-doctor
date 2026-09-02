import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Bed, Hospital, Page } from '../types';

const BED_COLOR: Record<string, string> = {
  AVAILABLE: 'badge-progress', OCCUPIED: 'badge-emergency', RESERVED: 'badge-hold',
  CLEANING: 'badge-open', MAINTENANCE: 'badge-closed', BLOCKED: 'badge-closed',
};

export default function HospitalsPage() {
  const [rows, setRows] = useState<Hospital[]>([]);
  const [q, setQ] = useState('');
  const [expanded, setExpanded] = useState<number | null>(null);
  const [beds, setBeds] = useState<Bed[]>([]);
  const [loading, setLoading] = useState(true);

  function load() {
    setLoading(true);
    api.get<Page<Hospital>>('/hospitals', { params: { q: q || undefined, size: 50 } })
      .then((r) => setRows(r.data.content)).finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, []);

  function toggle(h: Hospital) {
    if (expanded === h.id) { setExpanded(null); return; }
    setExpanded(h.id);
    api.get<Bed[]>(`/hospitals/${h.id}/beds`).then((r) => setBeds(r.data));
  }

  return (
    <div>
      <div className="page-head"><h2>Hospitals</h2></div>
      <div className="toolbar">
        <input className="input" style={{ maxWidth: 300 }} placeholder="Search name or city…" value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load()} />
        <button className="btn" onClick={load}>Search</button>
      </div>

      {loading ? <div className="spinner">Loading…</div> : rows.map((h) => (
        <div className="card" key={h.id} style={{ marginBottom: 14 }}>
          <div className="card-pad" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }} onClick={() => toggle(h)}>
            <div>
              <div style={{ fontWeight: 700, fontSize: 15 }}>{h.name} {h.partner && <span className="badge badge-open" style={{ fontSize: 10 }}>PARTNER</span>} {h.emergencyAvailable && <span className="badge badge-emergency" style={{ fontSize: 10 }}>24×7 ER</span>}</div>
              <div className="muted" style={{ fontSize: 12 }}>{h.city} · {h.phone} · {h.opdTiming}</div>
              <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginTop: 6 }}>{[...h.departments].map((d) => <span key={d} className="badge badge-normal" style={{ fontSize: 10 }}>{d}</span>)}</div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div className="mono" style={{ fontSize: 20, fontWeight: 700, color: 'var(--primary)' }}>{h.availableBeds}/{h.totalBeds}</div>
              <div className="muted" style={{ fontSize: 11 }}>beds available</div>
            </div>
          </div>
          {expanded === h.id && (
            <div className="card-pad" style={{ borderTop: '1px solid var(--border)' }}>
              {beds.length === 0 ? <div className="muted">No beds recorded.</div> : (
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  {beds.map((b) => (
                    <div key={b.id} style={{ border: '1px solid var(--border)', borderRadius: 8, padding: '8px 12px', minWidth: 90, textAlign: 'center' }}>
                      <div style={{ fontWeight: 600 }}>{b.bedNumber}</div>
                      <div className="muted" style={{ fontSize: 11 }}>{b.ward} · {b.type}</div>
                      <span className={'badge ' + (BED_COLOR[b.status] || 'badge-normal')} style={{ fontSize: 10, marginTop: 4 }}>{b.status}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
