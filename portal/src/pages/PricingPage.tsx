import { useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { RateCard, ServicePackage } from '../types';

export default function PricingPage() {
  const [rates, setRates] = useState<RateCard[]>([]);
  const [packages, setPackages] = useState<ServicePackage[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<Record<number, string>>({});
  const [error, setError] = useState('');

  function load() {
    setLoading(true);
    Promise.all([api.get<RateCard[]>('/rate-cards'), api.get<ServicePackage[]>('/packages')])
      .then(([r, p]) => { setRates(r.data); setPackages(p.data); })
      .finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, []);

  async function saveRate(rc: RateCard) {
    const val = editing[rc.id];
    if (val === undefined) return;
    setError('');
    try {
      await api.put('/rate-cards', { serviceType: rc.serviceType, label: rc.label, unit: rc.unit, unitRate: Number(val), active: true });
      setEditing((e) => { const n = { ...e }; delete n[rc.id]; return n; });
      load();
    } catch (e) { setError(apiErrorMessage(e)); }
  }

  if (loading) return <div className="spinner">Loading pricing…</div>;

  return (
    <div>
      <div className="page-head"><h2>Packages & Pricing</h2></div>
      {error && <div className="error-text">{error}</div>}

      <div className="grid-2">
        <div className="card">
          <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Rate Cards</h3><span className="muted" style={{ fontSize: 12 }}>Configurable inputs to the pricing engine — edit a rate and save.</span></div>
          <div className="table-wrap">
            <table className="data">
              <thead><tr><th>Service</th><th>Unit</th><th>Rate (₹)</th><th></th></tr></thead>
              <tbody>
                {rates.map((rc) => (
                  <tr key={rc.id}>
                    <td style={{ fontWeight: 600 }}>{rc.label}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{rc.serviceType}</div></td>
                    <td className="muted">{rc.unit}</td>
                    <td>
                      <input className="input" style={{ width: 110, padding: '6px 8px' }} type="number"
                             value={editing[rc.id] ?? rc.unitRate}
                             onChange={(e) => setEditing((ed) => ({ ...ed, [rc.id]: e.target.value }))} />
                    </td>
                    <td>{editing[rc.id] !== undefined && <button className="btn btn-sm btn-primary" onClick={() => saveRate(rc)}>Save</button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Packages</h3></div>
          <div className="card-pad">
            {packages.map((p) => (
              <div key={p.id} style={{ padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ fontWeight: 600 }}>{p.name}</div>
                    <div className="muted" style={{ fontSize: 12 }}>{p.description}</div>
                  </div>
                  <div className="mono" style={{ fontWeight: 700 }}>₹{p.indicativeTotal}</div>
                </div>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 8 }}>
                  {p.items.map((it, i) => <span key={i} className="badge badge-normal" style={{ fontSize: 11 }}>{it.label} · {it.quantity}×₹{it.unitPrice}</span>)}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
