import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Agent, CashCollection, CashInHandSummary } from '../types';

export default function CashReconciliationPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [agentId, setAgentId] = useState<number | null>(null);
  const [summary, setSummary] = useState<CashInHandSummary | null>(null);
  const [collections, setCollections] = useState<CashCollection[]>([]);
  const [pending, setPending] = useState<CashCollection[]>([]);
  const [loading, setLoading] = useState(true);
  const [agentLoading, setAgentLoading] = useState(false);
  const [error, setError] = useState('');

  // Record cash form.
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [caseNumber, setCaseNumber] = useState('');
  const [recording, setRecording] = useState(false);

  // Deposit.
  const [reference, setReference] = useState('');
  const [depositing, setDepositing] = useState(false);

  const loadPending = useCallback(() => {
    return api.get<CashCollection[]>('/cash/pending').then((r) => setPending(r.data));
  }, []);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([api.get<Agent[]>('/agents'), loadPending()])
      .then(([a]) => setAgents(a.data))
      .finally(() => setLoading(false));
  }, [loadPending]);

  useEffect(() => { load(); }, [load]);

  const loadAgent = useCallback((id: number) => {
    setAgentLoading(true);
    Promise.all([
      api.get<CashInHandSummary>(`/cash/agent/${id}/summary`),
      api.get<CashCollection[]>(`/cash/agent/${id}`),
    ]).then(([s, c]) => { setSummary(s.data); setCollections(c.data); })
      .finally(() => setAgentLoading(false));
  }, []);

  function selectAgent(value: string) {
    const id = value ? Number(value) : null;
    setAgentId(id);
    setSummary(null); setCollections([]);
    if (id != null) loadAgent(id);
  }

  const selectedAgent = agents.find((a) => a.id === agentId) || null;

  async function recordCash() {
    if (agentId == null) return;
    setRecording(true); setError('');
    try {
      await api.post('/cash/collect', {
        agentId, agentName: selectedAgent?.fullName,
        caseNumber: caseNumber || undefined,
        amount: Number(amount), note: note || undefined,
      });
      setAmount(''); setNote(''); setCaseNumber('');
      loadAgent(agentId); loadPending();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setRecording(false); }
  }

  async function depositAll() {
    if (agentId == null) return;
    setDepositing(true); setError('');
    try {
      await api.post('/cash/deposit', { agentId, reference: reference || undefined });
      setReference('');
      loadAgent(agentId); loadPending();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setDepositing(false); }
  }

  if (loading) return <div className="spinner">Loading cash desk…</div>;

  return (
    <div>
      <div className="page-head"><h2>Cash Reconciliation</h2></div>
      {error && <div className="error-text">{error}</div>}

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-pad" style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div className="field" style={{ minWidth: 240 }}>
            <label>Agent</label>
            <select className="input" value={agentId ?? ''} onChange={(e) => selectAgent(e.target.value)}>
              <option value="">Select an agent…</option>
              {agents.map((a) => <option key={a.id} value={a.id}>{a.fullName}</option>)}
            </select>
          </div>
        </div>
      </div>

      {agentId == null ? (
        <div className="card card-pad muted">Select an agent to view their cash summary.</div>
      ) : agentLoading ? (
        <div className="spinner">Loading agent cash…</div>
      ) : (
        <>
          <div className="stat-grid" style={{ marginBottom: 20 }}>
            <div className="card stat"><div className="label">Collected ₹</div><div className="value mono">{summary?.collected ?? 0}</div></div>
            <div className="card stat"><div className="label">Deposited ₹</div><div className="value mono">{summary?.deposited ?? 0}</div></div>
            <div className="card stat"><div className="label">In-hand ₹</div><div className="value primary mono">{summary?.inHand ?? 0}</div></div>
            <div className="card stat"><div className="label">Pending deposits</div><div className="value">{summary?.pendingDeposits ?? 0}</div></div>
          </div>

          <div className="card" style={{ marginBottom: 20 }}>
            <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Record cash</h3></div>
            <div className="card-pad" style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
              <div className="field" style={{ minWidth: 120 }}><label>Amount ₹</label><input className="input" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} /></div>
              <div className="field" style={{ minWidth: 140 }}><label>Case number (optional)</label><input className="input" value={caseNumber} onChange={(e) => setCaseNumber(e.target.value)} /></div>
              <div className="field" style={{ flex: 1, minWidth: 180 }}><label>Note (optional)</label><input className="input" value={note} onChange={(e) => setNote(e.target.value)} /></div>
              <button className="btn btn-primary" onClick={recordCash} disabled={recording || !amount}>{recording ? 'Recording…' : 'Record'}</button>
            </div>
            <div className="card-pad" style={{ borderTop: '1px solid var(--border)', display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
              <div className="field" style={{ flex: 1, minWidth: 180 }}><label>Deposit reference</label><input className="input" value={reference} onChange={(e) => setReference(e.target.value)} placeholder="Bank slip / txn ref" /></div>
              <button className="btn btn-primary" onClick={depositAll} disabled={depositing || !summary || summary.pendingDeposits === 0}>{depositing ? 'Depositing…' : 'Deposit all pending'}</button>
            </div>
          </div>

          <div className="card" style={{ marginBottom: 20 }}>
            <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>{selectedAgent?.fullName} — collections</h3></div>
            <div className="table-wrap">
              <table className="data">
                <thead><tr><th>Date</th><th>Amount</th><th>Case</th><th>Note</th><th>Deposited</th><th>Ref</th></tr></thead>
                <tbody>
                  {collections.length === 0 ? <tr><td colSpan={6} className="spinner">No collections</td></tr> : collections.map((c) => (
                    <tr key={c.id}>
                      <td className="muted">{new Date(c.createdAt).toLocaleString()}</td>
                      <td className="mono">₹{c.amount}</td>
                      <td className="mono">{c.caseNumber || '—'}</td>
                      <td className="muted">{c.note || '—'}</td>
                      <td>{c.deposited ? <span className="badge badge-progress">yes</span> : <span className="badge badge-hold">no</span>}{c.deposited && c.depositedAt && <div className="muted" style={{ fontSize: 10 }}>{new Date(c.depositedAt).toLocaleString()}</div>}</td>
                      <td className="muted">{c.depositReference || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      <div className="card">
        <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>All pending across agents</h3></div>
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Agent</th><th>Amount</th><th>Case</th><th>Date</th></tr></thead>
            <tbody>
              {pending.length === 0 ? <tr><td colSpan={4} className="spinner">No pending cash</td></tr> : pending.map((c) => (
                <tr key={c.id}>
                  <td style={{ fontWeight: 600 }}>{c.agentName || '#' + c.agentId}</td>
                  <td className="mono">₹{c.amount}</td>
                  <td className="mono">{c.caseNumber || '—'}</td>
                  <td className="muted">{new Date(c.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
