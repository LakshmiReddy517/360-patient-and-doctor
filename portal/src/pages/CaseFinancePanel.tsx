import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import { useLive } from '../api/live';
import type { Payment, Quote, ServicePackage } from '../types';

const QUOTE_BADGE: Record<string, string> = {
  DRAFT: 'badge-normal', SENT: 'badge-open', ACCEPTED: 'badge-progress',
  REJECTED: 'badge-closed', EXPIRED: 'badge-closed', PAID: 'badge-progress',
};

export default function CaseFinancePanel({ caseId }: { caseId: number }) {
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [payFor, setPayFor] = useState<Quote | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    Promise.all([
      api.get<Quote[]>('/quotes', { params: { caseId } }),
      api.get<Payment[]>('/payments', { params: { caseId } }),
    ]).then(([q, p]) => { setQuotes(q.data); setPayments(p.data); });
  }, [caseId]);
  useEffect(() => { load(); }, [load]);

  // Dynamic: refresh quotes/payments live when a billing event streams in for this case.
  useLive((name, data) => {
    if (name === 'event' && String(data?.caseId) === String(caseId) && (data?.source === 'BILLING')) load();
  });

  async function act(id: number, action: 'send' | 'accept' | 'reject') {
    setError('');
    try { await api.post(`/quotes/${id}/${action}`); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  return (
    <div>
      {error && <div className="error-text">{error}</div>}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <h3>Quotes</h3>
        <button className="btn btn-primary btn-sm" onClick={() => setShowCreate(true)}>+ New Quote</button>
      </div>

      {quotes.length === 0 ? <div className="card card-pad muted">No quotes yet.</div> : quotes.map((q) => (
        <div className="card card-pad" key={q.id} style={{ marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <strong>Quote #{q.id}</strong> <span className={'badge ' + (QUOTE_BADGE[q.status] || 'badge-normal')}>{q.status}</span>
            </div>
            <div className="mono" style={{ fontWeight: 700 }}>{q.currency} {q.total}</div>
          </div>
          <div className="table-wrap" style={{ marginTop: 10 }}>
            <table className="data">
              <thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead>
              <tbody>
                {q.items.map((it, i) => (
                  <tr key={i}><td>{it.label}</td><td>{it.quantity}</td><td>₹{it.unitPrice}</td><td>₹{it.amount}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ display: 'flex', gap: 20, marginTop: 10, flexWrap: 'wrap', fontSize: 13 }}>
            <span className="muted">Subtotal ₹{q.subtotal}</span>
            <span className="muted">Tax ({q.taxPercent}%) ₹{q.taxAmount}</span>
            <span className="muted">Discount ₹{q.discountAmount}</span>
            <span>Paid ₹{q.amountPaid}</span>
            <span style={{ fontWeight: 700 }}>Balance ₹{q.balance}</span>
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            {q.status === 'DRAFT' && <button className="btn btn-sm" onClick={() => act(q.id, 'send')}>Send</button>}
            {(q.status === 'SENT' || q.status === 'DRAFT') && <button className="btn btn-sm btn-primary" onClick={() => act(q.id, 'accept')}>Accept</button>}
            {q.status !== 'PAID' && q.status !== 'REJECTED' && <button className="btn btn-sm" onClick={() => act(q.id, 'reject')}>Reject</button>}
            {q.balance > 0 && <button className="btn btn-sm btn-primary" onClick={() => setPayFor(q)}>Record Payment</button>}
          </div>
        </div>
      ))}

      <h3 style={{ margin: '20px 0 12px' }}>Payments</h3>
      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Type</th><th>Amount</th><th>Method</th><th>Reference</th><th>By</th><th>When</th></tr></thead>
            <tbody>
              {payments.length === 0 ? <tr><td colSpan={6} className="spinner">No payments</td></tr> : payments.map((p) => (
                <tr key={p.id}>
                  <td><span className={'badge ' + (p.type === 'REFUND' ? 'badge-emergency' : 'badge-progress')}>{p.type}</span></td>
                  <td className="mono">₹{p.amount}</td>
                  <td className="muted">{p.method}</td>
                  <td className="muted">{p.reference || '—'}</td>
                  <td className="muted">{p.recordedBy}</td>
                  <td className="muted">{new Date(p.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showCreate && <CreateQuoteModal caseId={caseId} onClose={() => setShowCreate(false)} onCreated={() => { setShowCreate(false); load(); }} />}
      {payFor && <RecordPaymentModal quote={payFor} onClose={() => setPayFor(null)} onSaved={() => { setPayFor(null); load(); }} />}
    </div>
  );
}

const overlay: React.CSSProperties = { position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 };

function CreateQuoteModal({ caseId, onClose, onCreated }: { caseId: number; onClose: () => void; onCreated: () => void }) {
  const [packages, setPackages] = useState<ServicePackage[]>([]);
  const [packageId, setPackageId] = useState<number | ''>('');
  const [taxPercent, setTax] = useState('9');
  const [discountAmount, setDiscount] = useState('0');
  const [advanceAmount, setAdvance] = useState('0');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => { api.get<ServicePackage[]>('/packages').then((r) => setPackages(r.data)); }, []);

  async function save() {
    if (!packageId) { setError('Select a package'); return; }
    setSaving(true); setError('');
    try {
      await api.post('/quotes', { caseId, packageId, taxPercent: Number(taxPercent), discountAmount: Number(discountAmount), advanceAmount: Number(advanceAmount) });
      onCreated();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 460, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>New Quote</h3>
        {error && <div className="error-text">{error}</div>}
        <div className="field"><label>Package</label>
          <select className="input" value={packageId} onChange={(e) => setPackageId(Number(e.target.value))}>
            <option value="">Select a package…</option>
            {packages.map((p) => <option key={p.id} value={p.id}>{p.name} (indicative ₹{p.indicativeTotal})</option>)}
          </select>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Tax %</label><input className="input" type="number" value={taxPercent} onChange={(e) => setTax(e.target.value)} /></div>
          <div className="field" style={{ flex: 1 }}><label>Discount ₹</label><input className="input" type="number" value={discountAmount} onChange={(e) => setDiscount(e.target.value)} /></div>
          <div className="field" style={{ flex: 1 }}><label>Advance ₹</label><input className="input" type="number" value={advanceAmount} onChange={(e) => setAdvance(e.target.value)} /></div>
        </div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Creating…' : 'Create Quote'}</button>
        </div>
      </div>
    </div>
  );
}

function RecordPaymentModal({ quote, onClose, onSaved }: { quote: Quote; onClose: () => void; onSaved: () => void }) {
  const [amount, setAmount] = useState(String(quote.balance));
  const [type, setType] = useState('ADVANCE');
  const [method, setMethod] = useState('UPI');
  const [reference, setReference] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function save() {
    setSaving(true); setError('');
    try {
      await api.post('/payments', { caseId: quote.caseId, quoteId: quote.id, amount: Number(amount), type, method, reference });
      onSaved();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 420, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>Record Payment — Quote #{quote.id}</h3>
        {error && <div className="error-text">{error}</div>}
        <div className="field"><label>Amount (balance ₹{quote.balance})</label><input className="input" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} /></div>
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Type</label>
            <select className="input" value={type} onChange={(e) => setType(e.target.value)}><option>ADVANCE</option><option>BALANCE</option><option>REFUND</option></select>
          </div>
          <div className="field" style={{ flex: 1 }}><label>Method</label>
            <select className="input" value={method} onChange={(e) => setMethod(e.target.value)}><option>UPI</option><option>CASH</option><option>CARD</option><option>BANK_TRANSFER</option><option>WALLET</option></select>
          </div>
        </div>
        <div className="field"><label>Reference</label><input className="input" value={reference} onChange={(e) => setReference(e.target.value)} /></div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Record'}</button>
        </div>
      </div>
    </div>
  );
}
