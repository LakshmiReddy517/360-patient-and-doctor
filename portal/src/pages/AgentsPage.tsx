import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { useLive } from '../api/live';
import type { Agent, DispatchStats } from '../types';

const STATUS_BADGE: Record<string, string> = {
  AVAILABLE: 'badge-progress', ONLINE: 'badge-open', OFFLINE: 'badge-closed',
  JOB_OFFERED: 'badge-hold', SUSPENDED: 'badge-emergency',
};

export function agentBadge(s: string) {
  return STATUS_BADGE[s] || 'badge-open';
}

const EMT_LABEL: Record<string, string> = {
  NONE: '—', EMT_BASIC: 'EMT-B', EMT_INTERMEDIATE: 'EMT-I', EMT_PARAMEDIC: 'Paramedic',
};
const EMT_BADGE: Record<string, string> = {
  EMT_PARAMEDIC: 'badge-progress', EMT_INTERMEDIATE: 'badge-open', EMT_BASIC: 'badge-normal',
};

export default function AgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [stats, setStats] = useState<DispatchStats | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    Promise.all([api.get<Agent[]>('/agents'), api.get<DispatchStats>('/agents/stats')])
      .then(([a, s]) => { setAgents(a.data); setStats(s.data); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  // Dynamic: agent status/location update live.
  useLive((name) => { if (name === 'location' || name === 'event') load(); });

  if (loading) return <div className="spinner">Loading workforce…</div>;

  return (
    <div>
      <div className="page-head"><h2>Care Agents</h2></div>

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Available</div><div className="value primary">{stats?.available}</div></div>
        <div className="card stat"><div className="label">Online</div><div className="value">{stats?.online}</div></div>
        <div className="card stat"><div className="label">On Job</div><div className="value">{stats?.onJob}</div></div>
        <div className="card stat"><div className="label">Offline</div><div className="value">{stats?.offline}</div></div>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Agent</th><th>Code</th><th>EMT / Cert</th><th>Blood</th><th>Skills</th><th>Languages</th><th>Status</th><th>Workload</th><th>Rating</th><th>Verified</th></tr></thead>
            <tbody>
              {agents.map((a) => (
                <tr key={a.id}>
                  <td style={{ fontWeight: 600 }}>{a.fullName}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{a.mobile}</div></td>
                  <td className="mono">{a.employeeCode || '—'}</td>
                  <td>
                    {a.emtLevel && a.emtLevel !== 'NONE'
                      ? <span className={'badge ' + (EMT_BADGE[a.emtLevel] || 'badge-normal')}>{EMT_LABEL[a.emtLevel]}</span>
                      : <span className="muted">—</span>}
                    {a.licenceNumber && <div className="muted mono" style={{ fontSize: 10, marginTop: 2 }}>{a.licenceNumber}</div>}
                    {a.certificationExpiry && <div className="muted" style={{ fontSize: 10 }}>exp {a.certificationExpiry}</div>}
                  </td>
                  <td className="mono">{a.bloodGroup || '—'}</td>
                  <td><div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>{a.skills.map((s) => <span key={s} className="badge badge-normal" style={{ fontSize: 10 }}>{s}</span>)}</div></td>
                  <td className="muted" style={{ fontSize: 12 }}>{a.languages.join(', ')}</td>
                  <td><span className={'badge ' + agentBadge(a.status)}>{a.status.replace(/_/g, ' ')}</span></td>
                  <td className="muted">{a.activeAssignments} active</td>
                  <td className="muted">★ {a.rating.toFixed(1)}</td>
                  <td>{a.verified ? <span className="badge badge-progress">✓</span> : <span className="badge badge-hold">pending</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
