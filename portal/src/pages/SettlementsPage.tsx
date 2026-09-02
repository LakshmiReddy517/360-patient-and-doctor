import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Earning, PayeeType, Settlement, SettlementStats } from '../types';

const STATUS_BADGE: Record<string, string> = {
  PENDING: 'badge-hold', APPROVED: 'badge-open', PAID: 'badge-progress', CANCELLED: 'badge-closed',
};
const PAYEE_TYPES: PayeeType[] = ['AGENT', 'CARETAKER', 'AMBULANCE_PARTNER', 'HOSPITAL_PARTNER'];

export default function SettlementsPage() {
  const [settlements, setSettlements] = useState<Settlement[]>([]);
  const [earnings, setEarnings] = useState<Earning[]>([]);
  const [stats, setStats] = useState<SettlementStats | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      api.get<Settlement[]>('/settlements'),
      api.get<Earning[]>('/earnings/unsettled'),
      api.get<SettlementStats>('/settlements/stats'),
    ]).then(([s, e, st]) => { setSettlements(s.data); setEarnings(e.data); setStats(st.data); }).finally(() => setLoading(false));
  }, []);
  useEffect(() => { load(); }, [load]);

  async function setStatus(id: number, status: string, ref?: string) {
    setError('');
    try { await api.put(`/settlements/${id}/status`, { status, paymentReference: ref }); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  if (loading) return <div className="spinner">Loading settlements…</div>;

  return (
    <div>
      <div className="page-head">
        <h2>Settlements</h2>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ New Settlement</button>
      </div>
      {error && <div className="error-text">{error}</div>}

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Pending</div><div className="value">{stats?.pending}</div></div>
        <div className="card stat"><div className="label">Approved</div><div className="value">{stats?.approved}</div></div>
        <div className="card stat"><div className="label">Paid</div><div className="value primary">{stats?.paid}</div></div>
        <div className="card stat"><div className="label">Unsettled ₹</div><div className="value">{stats?.unsettledTotal}</div></div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Settlement batches</h3></div>
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>#</th><th>Payee</th><th>Period</th><th>Gross</th><th>Deduct</th><th>Net</th><th>Lines</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {settlements.length === 0 ? <tr><td colSpan={9} className="spinner">No settlements</td></tr> : settlements.map((s) => (
                <tr key={s.id}>
                  <td className="mono">#{s.id}</td>
                  <td style={{ fontWeight: 600 }}>{s.payeeName}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{s.payeeType.replace(/_/g, ' ')}</div></td>
                  <td className="muted">{s.periodLabel || '—'}</td>
                  <td className="mono">₹{s.grossAmount}</td>
                  <td className="mono">₹{s.deductions}</td>
                  <td className="mono" style={{ fontWeight: 700 }}>₹{s.netAmount}</td>
                  <td className="muted">{s.lineCount}</td>
                  <td><span className={'badge ' + (STATUS_BADGE[s.status] || 'badge-normal')}>{s.status}</span>{s.paymentReference && <div className="muted" style={{ fontSize: 10 }}>{s.paymentReference}</div>}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      {s.status === 'PENDING' && <button className="btn btn-sm" onClick={() => setStatus(s.id, 'APPROVED')}>Approve</button>}
                      {s.status === 'APPROVED' && <button className="btn btn-sm btn-primary" onClick={() => setStatus(s.id, 'PAID', prompt('Payment reference?') || 'PAID')}>Pay</button>}
                      {s.status !== 'PAID' && s.status !== 'CANCELLED' && <button className="btn btn-sm btn-danger" onClick={() => setStatus(s.id, 'CANCELLED')}>Cancel</button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Unsettled earnings</h3></div>
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Payee</th><th>Case</th><th>Description</th><th>Amount</th></tr></thead>
            <tbody>
              {earnings.length === 0 ? <tr><td colSpan={4} className="spinner">No unsettled earnings</td></tr> : earnings.map((e) => (
                <tr key={e.id}>
                  <td>{e.payeeName} <span className="muted">({e.payeeType.replace(/_/g, ' ')})</span></td>
                  <td className="mono">{e.caseNumber || '—'}</td>
                  <td className="muted">{e.description}</td>
                  <td className="mono">₹{e.amount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showCreate && <CreateModal payeeTypes={PAYEE_TYPES} onClose={() => setShowCreate(false)} onDone={() => { setShowCreate(false); load(); }} />}
    </div>
  );
}

function CreateModal({ payeeTypes, onClose, onDone }: { payeeTypes: PayeeType[]; onClose: () => void; onDone: () => void }) {
  const [payeeType, setPayeeType] = useState<PayeeType>('AGENT');
  const [payeeId, setPayeeId] = useState('1');
  const [payeeName, setPayeeName] = useState('');
  const [periodLabel, setPeriodLabel] = useState('');
  const [deductions, setDeductions] = useState('0');
  const [error, setError] = useState(''); const [saving, setSaving] = useState(false);

  async function save() {
    setSaving(true); setError('');
    try { await api.post('/settlements', { payeeType, payeeId: Number(payeeId), payeeName, periodLabel, deductions: Number(deductions) }); onDone(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }
  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 }} onClick={onClose}>
      <div className="card card-pad" style={{ width: 440, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>New Settlement</h3>
        <div className="muted" style={{ fontSize: 12, marginBottom: 12 }}>Bundles all unsettled earnings for the payee into one batch.</div>
        {error && <div className="error-text">{error}</div>}
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 2 }}><label>Payee type</label><select className="input" value={payeeType} onChange={(e) => setPayeeType(e.target.value as PayeeType)}>{payeeTypes.map((p) => <option key={p}>{p}</option>)}</select></div>
          <div className="field" style={{ flex: 1 }}><label>Payee id</label><input className="input" value={payeeId} onChange={(e) => setPayeeId(e.target.value)} /></div>
        </div>
        <div className="field"><label>Payee name</label><input className="input" value={payeeName} onChange={(e) => setPayeeName(e.target.value)} /></div>
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Period</label><input className="input" value={periodLabel} onChange={(e) => setPeriodLabel(e.target.value)} placeholder="Aug 2026" /></div>
          <div className="field" style={{ flex: 1 }}><label>Deductions ₹</label><input className="input" type="number" value={deductions} onChange={(e) => setDeductions(e.target.value)} /></div>
        </div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}><button className="btn" onClick={onClose}>Cancel</button><button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Creating…' : 'Create'}</button></div>
      </div>
    </div>
  );
}
