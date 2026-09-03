import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useLive } from '../api/live';
import type { CaseSummary, DashboardStats, Page } from '../types';
import { PriorityBadge, StatusBadge } from '../components/badges';

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recent, setRecent] = useState<CaseSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [live, setLive] = useState(false);
  const navigate = useNavigate();

  const load = useCallback(() => {
    Promise.all([
      api.get<DashboardStats>('/cases/dashboard'),
      api.get<Page<CaseSummary>>('/cases', { params: { size: 6 } }),
    ])
      .then(([s, c]) => { setStats(s.data); setRecent(c.data.content); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  // Real-time: refresh the dashboard whenever a case/request event streams in.
  useLive((name) => {
    if (name === 'hello') { setLive(true); return; }
    if (name === 'event' || name === 'request') load();
  });

  if (loading) return <div className="spinner">Loading dashboard…</div>;

  return (
    <div>
      <div className="page-head">
        <h2>Operations Dashboard</h2>
        {live && (
          <span className="badge" style={{ background: 'var(--success-soft)', color: 'var(--success)' }}>
            <span style={{ width: 8, height: 8, borderRadius: 50, background: 'var(--success)', display: 'inline-block', animation: 'pulse 1.4s infinite' }} /> LIVE
          </span>
        )}
      </div>

      <div className="stat-grid">
        <div className="card stat"><div className="label">Open Cases</div><div className="value primary">{stats?.openCases}</div></div>
        <div className="card stat"><div className="label">In Progress</div><div className="value">{stats?.inProgressCases}</div></div>
        <div className="card stat"><div className="label">On Hold</div><div className="value">{stats?.onHoldCases}</div></div>
        <div className="card stat"><div className="label">Emergencies</div><div className="value danger">{stats?.emergencies}</div></div>
        <div className="card stat"><div className="label">SLA Breached</div><div className={'value' + ((stats?.slaBreached ?? 0) > 0 ? ' danger' : '')}>{stats?.slaBreached ?? 0}</div></div>
        <div className="card stat"><div className="label">Closed</div><div className="value">{stats?.closedCases}</div></div>
        <div className="card stat"><div className="label">Total Cases</div><div className="value">{stats?.totalCases}</div></div>
      </div>

      <div className="card" style={{ marginTop: 22 }}>
        <div className="card-pad" style={{ borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3>Recent Cases</h3>
          <span className="link" onClick={() => navigate('/cases')}>View all →</span>
        </div>
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr><th>Case #</th><th>Patient</th><th>Title</th><th>Priority</th><th>Status</th></tr>
            </thead>
            <tbody>
              {recent.map((c) => (
                <tr key={c.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/cases/${c.id}`)}>
                  <td className="mono">{c.caseNumber}</td>
                  <td>{c.patientName}</td>
                  <td className="muted">{c.title || '—'}</td>
                  <td><PriorityBadge priority={c.priority} emergency={c.emergency} /></td>
                  <td><StatusBadge status={c.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
