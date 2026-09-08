import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { useLive } from '../api/live';
import type { DashboardStats, OpsDashboard } from '../types';

/**
 * Operational reports (blueprint points 6 & 68): the full Command-Centre KPI set —
 * requests, cases, emergencies, SLA, workforce, fleet and today's pickups — in one place.
 */
export default function ReportsPage() {
  const [ops, setOps] = useState<OpsDashboard | null>(null);
  const [cases, setCases] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    Promise.all([
      api.get<OpsDashboard>('/ops/dashboard'),
      api.get<DashboardStats>('/cases/dashboard'),
    ]).then(([o, c]) => { setOps(o.data); setCases(c.data); }).finally(() => setLoading(false));
  }, []);
  useEffect(() => { load(); }, [load]);
  useLive((name) => { if (name === 'event' || name === 'request' || name === 'location') load(); });

  if (loading) return <div className="spinner">Loading reports…</div>;
  if (!ops) return null;

  const Section = ({ title, cards }: { title: string; cards: { label: string; value: number; tone?: string }[] }) => (
    <div style={{ marginBottom: 24 }}>
      <h3 style={{ margin: '4px 0 12px' }}>{title}</h3>
      <div className="stat-grid">
        {cards.map((c) => (
          <div className="card stat" key={c.label}>
            <div className="label">{c.label}</div>
            <div className={'value' + (c.tone ? ' ' + c.tone : '')}>{c.value}</div>
          </div>
        ))}
      </div>
    </div>
  );

  return (
    <div>
      <div className="page-head"><h2>Operational Reports</h2></div>

      <Section title="Demand & Cases" cards={[
        { label: 'New Requests', value: ops.newRequests, tone: 'primary' },
        { label: 'Open Cases', value: ops.openCases },
        { label: 'In Progress', value: ops.inProgressCases },
        { label: 'On Hold', value: ops.onHoldCases },
        { label: 'Emergencies', value: ops.emergencies, tone: 'danger' },
        { label: 'SLA Breached', value: cases?.slaBreached ?? 0, tone: (cases?.slaBreached ?? 0) > 0 ? 'danger' : '' },
        { label: 'Closed', value: ops.closedCases },
        { label: 'Total Cases', value: ops.totalCases },
      ]} />

      <Section title="Dispatch & Workforce" cards={[
        { label: 'Active Dispatches', value: ops.activeDispatches, tone: 'primary' },
        { label: 'Pickups Today', value: ops.pickupsToday },
        { label: 'Agents Online', value: ops.agentsOnline },
        { label: 'Agents Available', value: ops.agentsAvailable },
        { label: 'Agents On Job', value: ops.agentsOnJob },
        { label: 'Agents Offline', value: ops.agentsOffline },
      ]} />

      <Section title="Ambulance Fleet" cards={[
        { label: 'Total Vehicles', value: ops.ambulancesTotal },
        { label: 'Available', value: ops.ambulancesAvailable, tone: 'primary' },
        { label: 'On Trip', value: ops.ambulancesOnTrip },
        { label: 'Maintenance', value: ops.ambulancesMaintenance },
      ]} />
    </div>
  );
}
