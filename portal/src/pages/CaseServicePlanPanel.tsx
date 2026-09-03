import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ServicePlan } from '../types';

const CAT_BADGE: Record<string, string> = {
  Billing: 'badge-open', Dispatch: 'badge-progress', Transport: 'badge-open',
  Healthcare: 'badge-progress', Care: 'badge-hold', Clinical: 'badge-normal',
};

export default function CaseServicePlanPanel({ caseId }: { caseId: number }) {
  const [plan, setPlan] = useState<ServicePlan | null>(null);
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState<{ summary: string; disclaimer: string } | null>(null);
  const [genLoading, setGenLoading] = useState(false);

  useEffect(() => {
    api.get<ServicePlan>(`/cases/${caseId}/service-plan`).then((r) => setPlan(r.data)).finally(() => setLoading(false));
  }, [caseId]);

  async function generateSummary() {
    setGenLoading(true);
    try { const r = await api.get<{ summary: string; disclaimer: string }>(`/cases/${caseId}/ai-summary`); setSummary(r.data); }
    finally { setGenLoading(false); }
  }

  if (loading) return <div className="spinner">Loading service plan…</div>;
  if (!plan) return null;

  return (
    <div>
      <div className="card card-pad" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3>Service Plan — {plan.caseNumber}</h3>
            <div className="muted" style={{ fontSize: 13 }}>
              {plan.deliveredItems}/{plan.totalItems} service{plan.totalItems === 1 ? '' : 's'} delivered across all modules
            </div>
          </div>
        </div>
        <div className="stat-grid" style={{ marginTop: 14 }}>
          <div className="card stat"><div className="label">Planned value</div><div className="value mono">₹{plan.plannedValue}</div></div>
          <div className="card stat"><div className="label">Delivered value</div><div className="value mono primary">₹{plan.deliveredValue}</div></div>
          <div className="card stat"><div className="label">Variance (pending)</div><div className="value mono">₹{plan.varianceValue}</div></div>
          <div className="card stat"><div className="label">Progress</div><div className="value">{plan.totalItems ? Math.round((plan.deliveredItems / plan.totalItems) * 100) : 0}%</div></div>
        </div>
      </div>

      <div className="card card-pad" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3>✨ AI Case Summary</h3>
          <button className="btn btn-sm btn-primary" onClick={generateSummary} disabled={genLoading}>{genLoading ? 'Generating…' : 'Generate'}</button>
        </div>
        {summary ? (
          <>
            <p style={{ marginTop: 12, lineHeight: 1.7 }}>{summary.summary}</p>
            <div style={{ background: 'var(--warning-soft)', color: 'var(--warning)', padding: '8px 12px', borderRadius: 8, fontSize: 11.5, marginTop: 8 }}>⚕ {summary.disclaimer}</div>
          </>
        ) : <div className="muted" style={{ marginTop: 8, fontSize: 13 }}>Generate an operational summary of this case (guardrailed — no clinical advice).</div>}
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Area</th><th>Service</th><th>Resource</th><th>Status</th><th>Delivery</th><th>Planned</th><th>Delivered</th></tr></thead>
            <tbody>
              {plan.items.length === 0 ? <tr><td colSpan={7} className="spinner">No services committed yet.</td></tr>
                : plan.items.map((it, i) => (
                  <tr key={i}>
                    <td><span className={'badge ' + (CAT_BADGE[it.category] || 'badge-normal')}>{it.category}</span></td>
                    <td style={{ fontWeight: 600 }}>{it.title.replace(/_/g, ' ')}</td>
                    <td className="muted">{it.detail || '—'}</td>
                    <td><span className="badge badge-normal">{it.status.replace(/_/g, ' ')}</span></td>
                    <td>{it.delivered
                      ? <span className="badge badge-progress">✓ delivered</span>
                      : <span className="badge badge-hold">pending</span>}</td>
                    <td className="mono">{it.plannedAmount ? '₹' + it.plannedAmount : '—'}</td>
                    <td className="mono">{it.actualAmount ? '₹' + it.actualAmount : '—'}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
