import { useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { FareEstimate, RateCard, ServicePackage, ServiceType } from '../types';

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

      <FareEstimator rates={rates} />

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

/** Multi-factor fare estimate (point 17): base + per-km distance, emergency multiplier, night surcharge. */
function FareEstimator({ rates }: { rates: RateCard[] }) {
  const priced = rates.filter((r) => (r.baseFare ?? 0) > 0 || (r.perKmRate ?? 0) > 0);
  const [serviceType, setServiceType] = useState<ServiceType>((priced[0]?.serviceType as ServiceType) || 'AMBULANCE');
  const [distanceKm, setDistanceKm] = useState('12');
  const [emergency, setEmergency] = useState(false);
  const [night, setNight] = useState(false);
  const [est, setEst] = useState<FareEstimate | null>(null);
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function estimate() {
    setErr(''); setBusy(true);
    try {
      const r = await api.get<FareEstimate>('/pricing/estimate', {
        params: { serviceType, distanceKm: Number(distanceKm) || 0, emergency, night },
      });
      setEst(r.data);
    } catch (e) { setErr(apiErrorMessage(e)); setEst(null); } finally { setBusy(false); }
  }

  const options = (priced.length ? priced : rates);

  return (
    <div className="card card-pad" style={{ marginBottom: 22 }}>
      <h3 style={{ marginBottom: 4 }}>Fare Estimator</h3>
      <div className="muted" style={{ fontSize: 12, marginBottom: 14 }}>
        Multi-factor pricing — base fare + per-km distance, an emergency multiplier, and a night surcharge.
      </div>
      {err && <div className="error-text">{err}</div>}
      <div className="toolbar" style={{ flexWrap: 'wrap' }}>
        <select className="input" style={{ maxWidth: 240 }} value={serviceType} onChange={(e) => setServiceType(e.target.value as ServiceType)}>
          {options.map((r) => <option key={r.id} value={r.serviceType}>{r.label}</option>)}
        </select>
        <input className="input" style={{ maxWidth: 140 }} type="number" placeholder="Distance km"
               value={distanceKm} onChange={(e) => setDistanceKm(e.target.value)} />
        <label style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 13 }}>
          <input type="checkbox" checked={emergency} onChange={(e) => setEmergency(e.target.checked)} /> Emergency
        </label>
        <label style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 13 }}>
          <input type="checkbox" checked={night} onChange={(e) => setNight(e.target.checked)} /> Night (22:00–06:00)
        </label>
        <button className="btn btn-primary" onClick={estimate} disabled={busy}>{busy ? 'Estimating…' : 'Estimate fare'}</button>
      </div>

      {est && (
        <div style={{ display: 'flex', gap: 24, alignItems: 'flex-end', marginTop: 16, flexWrap: 'wrap' }}>
          <div style={{ fontSize: 13, lineHeight: 1.9 }}>
            <div>Base fare <span className="mono">₹{est.baseFare}</span></div>
            <div>Distance ({est.distanceKm} km) <span className="mono">+ ₹{est.distanceCharge}</span></div>
            <div className="muted">Subtotal <span className="mono">₹{est.subtotal}</span></div>
            {est.emergencySurcharge > 0 && <div>Emergency surcharge <span className="mono">+ ₹{est.emergencySurcharge}</span></div>}
            {est.nightSurcharge > 0 && <div>Night surcharge <span className="mono">+ ₹{est.nightSurcharge}</span></div>}
          </div>
          <div style={{ textAlign: 'right' }}>
            <div className="muted" style={{ fontSize: 12 }}>Estimated fare</div>
            <div className="mono" style={{ fontSize: 28, fontWeight: 700, color: 'var(--primary)' }}>₹{est.total}</div>
          </div>
        </div>
      )}
    </div>
  );
}
