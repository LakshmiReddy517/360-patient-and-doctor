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
      <div className="card card-pad" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h3>Service Plan — {plan.caseNumber}</h3>
          <div className="muted" style={{ fontSize: 13 }}>{plan.totalItems} committed service{plan.totalItems === 1 ? '' : 's'} across all modules</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="muted" style={{ fontSize: 12 }}>Committed value</div>
          <div className="mono" style={{ fontSize: 24, fontWeight: 700, color: 'var(--primary)' }}>₹{plan.committedValue}</div>
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
            <thead><tr><th>Area</th><th>Service</th><th>Resource</th><th>Status</th><th>Value</th></tr></thead>
            <tbody>
              {plan.items.length === 0 ? <tr><td colSpan={5} className="spinner">No services committed yet.</td></tr>
                : plan.items.map((it, i) => (
                  <tr key={i}>
                    <td><span className={'badge ' + (CAT_BADGE[it.category] || 'badge-normal')}>{it.category}</span></td>
                    <td style={{ fontWeight: 600 }}>{it.title.replace(/_/g, ' ')}</td>
                    <td className="muted">{it.detail || '—'}</td>
                    <td><span className="badge badge-normal">{it.status.replace(/_/g, ' ')}</span></td>
                    <td className="mono">{it.amount != null ? '₹' + it.amount : '—'}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
