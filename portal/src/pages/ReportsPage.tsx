import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { useLive } from '../api/live';
import type { DashboardStats, OpsDashboard } from '../types';

/* ---- flat chart primitives (no shadow boxes) ---- */
function Donut({ segs, center, label }: { segs: { label: string; value: number; color: string }[]; center: number; label: string }) {
  const r = 50, c = 2 * Math.PI * r, total = segs.reduce((s, d) => s + d.value, 0) || 1;
  let acc = 0;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
      <div style={{ position: 'relative', flex: 'none' }}>
        <svg width="128" height="128" viewBox="0 0 128 128">
          <circle cx="64" cy="64" r={r} fill="none" stroke="#efe9dd" strokeWidth="13" />
          {segs.map((d, i) => { const len = (d.value / total) * c; const el = <circle key={i} cx="64" cy="64" r={r} fill="none" stroke={d.color} strokeWidth="13" strokeLinecap="round" strokeDasharray={`${len} ${c - len}`} strokeDashoffset={-acc} transform="rotate(-90 64 64)" />; acc += len; return el; })}
        </svg>
        <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', textAlign: 'center' }}>
          <div><b style={{ fontSize: 23, fontWeight: 800, color: '#223733' }}>{center}</b><div style={{ fontSize: 11, color: '#8a9893' }}>{label}</div></div>
        </div>
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 9 }}>
        {segs.map((d) => <div key={d.label} style={{ display: 'flex', alignItems: 'center', gap: 9, fontSize: 13, color: '#4a5a55' }}><span style={{ width: 11, height: 11, borderRadius: 3, background: d.color }} />{d.label}<b style={{ marginLeft: 'auto', color: '#223733' }}>{d.value}</b></div>)}
      </div>
    </div>
  );
}
function HBars({ items }: { items: { label: string; value: number; color: string }[] }) {
  const max = Math.max(1, ...items.map((i) => i.value));
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 13 }}>
      {items.map((it) => (
        <div key={it.label}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12.5, color: '#4a5a55', marginBottom: 5 }}><span>{it.label}</span><b style={{ color: '#223733' }}>{it.value}</b></div>
          <div style={{ height: 9, borderRadius: 99, background: '#efe9dd', overflow: 'hidden' }}><div style={{ height: '100%', width: `${(it.value / max) * 100}%`, minWidth: it.value ? 6 : 0, borderRadius: 99, background: it.color }} /></div>
        </div>
      ))}
    </div>
  );
}

export default function ReportsPage() {
  const [ops, setOps] = useState<OpsDashboard | null>(null);
  const [cases, setCases] = useState<DashboardStats | null>(null);
  const [agents, setAgents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    Promise.all([api.get<OpsDashboard>('/ops/dashboard'), api.get<DashboardStats>('/cases/dashboard'), api.get<any[]>('/agents').catch(() => ({ data: [] }))])
      .then(([o, c, a]) => { setOps(o.data); setCases(c.data); setAgents(a.data || []); }).finally(() => setLoading(false));
  }, []);
  useEffect(() => { load(); }, [load]);
  useLive((name) => { if (name === 'event' || name === 'request' || name === 'location') load(); });

  if (loading) return <div className="spinner">Loading reports…</div>;
  if (!ops) return null;

  // consistent workforce counts from the live roster (on-job ⇒ online)
  const wfOffline = agents.filter((a) => a.status === 'OFFLINE').length;
  const wfAvailable = agents.filter((a) => a.status === 'AVAILABLE').length;
  const wfOnJob = agents.filter((a) => a.status && !['AVAILABLE', 'OFFLINE'].includes(a.status)).length;
  const wfOnline = agents.length ? agents.length - wfOffline : ops.agentsOnline;

  const kpis = [
    { label: 'New Requests', value: ops.newRequests, c: '#3b7fd0' },
    { label: 'Open Cases', value: ops.openCases, c: '#0f766e' },
    { label: 'Emergencies', value: ops.emergencies, c: '#d9453f' },
    { label: 'SLA Breached', value: cases?.slaBreached ?? 0, c: (cases?.slaBreached ?? 0) > 0 ? '#d9453f' : '#12876f' },
    { label: 'Active Dispatches', value: ops.activeDispatches, c: '#d9813f' },
    { label: 'Pickups Today', value: ops.pickupsToday, c: '#7a68c9' },
  ];

  const exportCsv = () => {
    const rows: [string, number][] = [
      ['New Requests', ops.newRequests], ['Open Cases', ops.openCases], ['In Progress', ops.inProgressCases],
      ['On Hold', ops.onHoldCases], ['Emergencies', ops.emergencies], ['SLA Breached', cases?.slaBreached ?? 0],
      ['Closed', ops.closedCases], ['Total Cases', ops.totalCases], ['Active Dispatches', ops.activeDispatches],
      ['Pickups Today', ops.pickupsToday], ['Agents Online', ops.agentsOnline], ['Agents Available', ops.agentsAvailable],
      ['Agents On Job', ops.agentsOnJob], ['Agents Offline', ops.agentsOffline], ['Ambulances Total', ops.ambulancesTotal],
      ['Ambulances Available', ops.ambulancesAvailable], ['On Trip', ops.ambulancesOnTrip], ['Maintenance', ops.ambulancesMaintenance],
    ];
    const csv = 'Metric,Value\n' + rows.map((r) => `${r[0]},${r[1]}`).join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a'); a.href = url; a.download = 'operational-report.csv'; a.click(); URL.revokeObjectURL(url);
  };

  return (
    <div>
      <div className="page-head">
        <div><h2>Operational Reports</h2><div className="muted" style={{ fontSize: 13, marginTop: 2 }}>Command-centre KPIs, pipeline and resource utilisation</div></div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <span className="rpill">● Live</span>
          <button className="btn" onClick={exportCsv}>⭳ Export CSV</button>
        </div>
      </div>

      {/* KPI strip — flat */}
      <div className="rkpis">
        {kpis.map((k) => (
          <div className="rkpi" key={k.label}><span className="rkpi-bar" style={{ background: k.c }} /><div className="rkpi-v" style={{ color: k.c }}>{k.value}</div><div className="rkpi-l">{k.label}</div></div>
        ))}
      </div>

      {/* charts — flat, no shadow */}
      <div className="rgrid">
        <div className="rcard">
          <h3>Case Pipeline</h3>
          <Donut center={ops.totalCases} label="cases" segs={[
            { label: 'Open', value: ops.openCases, color: '#2f9e8f' },
            { label: 'In Progress', value: ops.inProgressCases, color: '#5b9bd5' },
            { label: 'On Hold', value: ops.onHoldCases, color: '#e8955a' },
            { label: 'Closed', value: ops.closedCases, color: '#c2c6cd' },
          ]} />
        </div>
        <div className="rcard">
          <h3>Ambulance Fleet</h3>
          <Donut center={ops.ambulancesTotal} label="vehicles" segs={[
            { label: 'Available', value: ops.ambulancesAvailable, color: '#2f9e8f' },
            { label: 'On Trip', value: ops.ambulancesOnTrip, color: '#5b9bd5' },
            { label: 'Maintenance', value: ops.ambulancesMaintenance, color: '#e8955a' },
          ]} />
        </div>
        <div className="rcard">
          <h3>Workforce Status</h3>
          <HBars items={[
            { label: 'Online', value: wfOnline, color: '#3b7fd0' },
            { label: 'Available', value: wfAvailable, color: '#12876f' },
            { label: 'On Job', value: wfOnJob, color: '#d9813f' },
            { label: 'Offline', value: wfOffline, color: '#b4bcb6' },
          ]} />
        </div>
      </div>

      <div className="rcard" style={{ marginTop: 18 }}>
        <h3>Demand Snapshot</h3>
        <div className="rgrid2">
          <HBars items={[
            { label: 'New Requests', value: ops.newRequests, color: '#3b7fd0' },
            { label: 'Open Cases', value: ops.openCases, color: '#2f9e8f' },
            { label: 'In Progress', value: ops.inProgressCases, color: '#5b9bd5' },
          ]} />
          <HBars items={[
            { label: 'Closed', value: ops.closedCases, color: '#8a9b94' },
            { label: 'Emergencies', value: ops.emergencies, color: '#d9453f' },
            { label: 'SLA Breached', value: cases?.slaBreached ?? 0, color: (cases?.slaBreached ?? 0) > 0 ? '#d9453f' : '#12876f' },
          ]} />
        </div>
      </div>
    </div>
  );
}
