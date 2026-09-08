import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Consent, ConsentStatus, Page, PatientSummary } from '../types';

const STATUS_BADGE: Record<ConsentStatus, string> = {
  GRANTED: 'badge-progress', REVOKED: 'badge-closed', EXPIRED: 'badge-hold',
};

interface ConsentRow extends Consent {
  patientName: string;
}

export default function ConsentPage() {
  const [rows, setRows] = useState<ConsentRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const patients = (await api.get<Page<PatientSummary>>('/patients', { params: { size: 100 } })).data.content;
      const lists = await Promise.all(
        patients.map((p) => api.get<Consent[]>(`/patients/${p.id}/consents`).then((r) => ({ p, consents: r.data }))),
      );
      const flat: ConsentRow[] = [];
      for (const { p, consents } of lists) {
        for (const c of consents) flat.push({ ...c, patientName: p.fullName });
      }
      flat.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''));
      setRows(flat);
    } catch (e) { setError(apiErrorMessage(e)); } finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);

  async function revoke(patientId: number, consentId: number) {
    setError('');
    try { await api.post(`/patients/${patientId}/consents/${consentId}/revoke`); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  const active = rows.filter((r) => r.status === 'GRANTED').length;
  const revoked = rows.filter((r) => r.status === 'REVOKED').length;

  if (loading) return <div className="spinner">Loading consents…</div>;

  return (
    <div>
      <div className="page-head">
        <h2>Consent Management</h2>
      </div>

      {error && <div className="error-text">{error}</div>}

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Total consents</div><div className="value primary">{rows.length}</div></div>
        <div className="card stat"><div className="label">Active (granted)</div><div className="value">{active}</div></div>
        <div className="card stat"><div className="label">Revoked</div><div className="value">{revoked}</div></div>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Patient</th><th>Scope</th><th>Granted to</th><th>Purpose</th><th>Valid to</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {rows.length === 0 ? <tr><td colSpan={7} className="spinner">No consent records</td></tr> : rows.map((c) => (
                <tr key={c.patientId + '-' + c.id}>
                  <td style={{ fontWeight: 600 }}>{c.patientName}</td>
                  <td>{c.scope}</td>
                  <td className="muted">{c.grantedTo}</td>
                  <td className="muted">{c.purpose || '—'}</td>
                  <td className="muted">{c.validTo || '—'}</td>
                  <td><span className={'badge ' + (STATUS_BADGE[c.status] || 'badge-normal')}>{c.status}</span></td>
                  <td>{c.status === 'GRANTED' && <span className="link" onClick={() => revoke(c.patientId, c.id)}>Revoke</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
