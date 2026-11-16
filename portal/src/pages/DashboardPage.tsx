import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useLive } from '../api/live';

/* ---------- chart helpers ---------- */
function smoothPath(pts: [number, number][]) {
  if (pts.length < 2) return pts.length === 1 ? `M ${pts[0][0]} ${pts[0][1]}` : '';
  let d = `M ${pts[0][0]} ${pts[0][1]}`;
  for (let i = 0; i < pts.length - 1; i++) {
    const [x0, y0] = pts[i]; const [x1, y1] = pts[i + 1]; const cx = (x0 + x1) / 2;
    d += ` C ${cx} ${y0}, ${cx} ${y1}, ${x1} ${y1}`;
  }
  return d;
}
const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

function LineChart({ a, b }: { a: number[]; b: number[] }) {
  const W = 340, H = 190, padL = 28, padR = 14, padT = 14, padB = 28;
  const max = Math.max(5, ...a, ...b);
  const x = (i: number) => padL + (i * (W - padL - padR)) / (DAYS.length - 1);
  const y = (v: number) => padT + (1 - v / max) * (H - padT - padB);
  const pa = a.map((v, i) => [x(i), y(v)] as [number, number]);
  const pb = b.map((v, i) => [x(i), y(v)] as [number, number]);
  const area = (pts: [number, number][]) => smoothPath(pts) + ` L ${x(DAYS.length - 1)} ${H - padB} L ${x(0)} ${H - padB} Z`;
  const empty = a.every((v) => v === 0) && b.every((v) => v === 0);
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }}>
      <defs>
        <linearGradient id="g1" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#2f9e8f" stopOpacity=".22" /><stop offset="1" stopColor="#2f9e8f" stopOpacity="0" /></linearGradient>
        <linearGradient id="g2" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#7cc47a" stopOpacity=".2" /><stop offset="1" stopColor="#7cc47a" stopOpacity="0" /></linearGradient>
      </defs>
      {[0, 0.25, 0.5, 0.75, 1].map((f) => { const v = Math.round(max * (1 - f)); return (
        <g key={f}><line x1={padL} y1={padT + f * (H - padT - padB)} x2={W - padR} y2={padT + f * (H - padT - padB)} stroke="#eef0ec" strokeWidth="1" /><text x={padL - 6} y={padT + f * (H - padT - padB) + 3} textAnchor="end" fontSize="9" fill="#9aa39f">{v}</text></g>); })}
      <path d={area(pb)} fill="url(#g2)" /><path d={area(pa)} fill="url(#g1)" />
      <path d={smoothPath(pb)} fill="none" stroke="#7cc47a" strokeWidth="2.4" />
      <path d={smoothPath(pa)} fill="none" stroke="#2f9e8f" strokeWidth="2.6" />
      {pa.map(([cx, cy], i) => <circle key={i} cx={cx} cy={cy} r="2.6" fill="#2f9e8f" />)}
      {DAYS.map((d, i) => <text key={d} x={x(i)} y={H - 9} textAnchor="middle" fontSize="10" fill="#9aa39f">{d}</text>)}
      {empty && <text x={W / 2} y={H / 2} textAnchor="middle" fontSize="12" fill="#b4bcb6">No activity this week yet</text>}
    </svg>
  );
}

function Donut({ segs, center, label }: { segs: { label: string; value: number; color: string }[]; center: number; label: string }) {
  const r = 52, c = 2 * Math.PI * r, total = segs.reduce((s, d) => s + d.value, 0) || 1;
  let acc = 0;
  return (
    <div className="donut-wrap">
      <div className="center">
        <svg width="132" height="132" viewBox="0 0 132 132">
          <circle cx="66" cy="66" r={r} fill="none" stroke="#f0f1ee" strokeWidth="14" />
          {segs.map((d, i) => { const len = (d.value / total) * c; const el = <circle key={i} cx="66" cy="66" r={r} fill="none" stroke={d.color} strokeWidth="14" strokeLinecap="round" strokeDasharray={`${len} ${c - len}`} strokeDashoffset={-acc} transform="rotate(-90 66 66)" />; acc += len; return el; })}
        </svg>
        <div className="big"><div><b>{center}</b><span>{label}</span></div></div>
      </div>
      <div className="leg flex1">
        {segs.map((d) => <div className="row" key={d.label}><span className="sw" style={{ background: d.color }} />{d.label}<span className="ct">{d.value}</span></div>)}
      </div>
    </div>
  );
}

function BarChart({ bars }: { bars: { label: string; value: number; color: string }[] }) {
  const max = Math.max(1, ...bars.map((b) => b.value)), H = 180;
  if (!bars.length) return <div style={{ height: H, display: 'grid', placeItems: 'center', color: '#b4bcb6', fontSize: 13 }}>No request data yet</div>;
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-around', height: H, gap: 6, padding: '8px 4px 0' }}>
      {bars.map((b) => (
        <div key={b.label} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', height: '100%', justifyContent: 'flex-end' }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: '#223733', marginBottom: 5 }}>{b.value}</div>
          <div style={{ width: '58%', maxWidth: 34, height: `${(b.value / max) * (H - 48)}px`, minHeight: 4, background: b.color, borderRadius: '7px 7px 3px 3px' }} />
          <div style={{ fontSize: 10, color: '#8a9893', marginTop: 7, textAlign: 'center', whiteSpace: 'pre-line', lineHeight: 1.15 }}>{b.label}</div>
        </div>
      ))}
    </div>
  );
}

/* ---------- formatting ---------- */
const titleCase = (s: string) => (s || '').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (m) => m.toUpperCase());
const SERVICE_COLORS: Record<string, string> = { AMBULANCE: '#6cc3a0', HOSPITAL_ADMISSION: '#6aa9e0', DOCTOR_APPOINTMENT: '#eca96a', CARETAKER: '#a99ce0', PICKUP_DROP: '#67c0b0', LOCAL_TRANSPORT: '#7fb5e6', ACCOMMODATION: '#d6a15a', FOOD: '#c2c6cd' };
const palette = ['#6cc3a0', '#6aa9e0', '#eca96a', '#a99ce0', '#c2c6cd'];
const statusChip = (s: string) => {
  const u = (s || '').toUpperCase();
  if (u.includes('ROUTE') || u.includes('TRANSIT') || u.includes('ASSIGN')) return 'chip-route';
  if (u.includes('PROGRESS') || u.includes('PICK') || u.includes('ARRIV')) return 'chip-prog';
  if (u.includes('CLOSED') || u.includes('COMPLETE') || u.includes('CONFIRM')) return 'chip-done';
  return 'chip-assigned';
};
const AV = ['#2f9e8f', '#5b9bd5', '#e8955a', '#9b8fd0', '#67c0b0'];
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/* ---------- skeletons ---------- */
const Sk = ({ w, h, r = 8, mt = 0 }: { w: number | string; h: number; r?: number; mt?: number }) => <div className="skel" style={{ width: w, height: h, borderRadius: r, marginTop: mt }} />;
const SkTile = () => (<div className="dcard" style={{ minHeight: 142 }}><Sk w={46} h={46} r={13} /><Sk w={72} h={30} mt={18} /><Sk w="58%" h={13} mt={11} /><Sk w={54} h={12} mt={9} /></div>);
const SkChart = () => (<div className="dcard"><div className="dcard-head"><Sk w={140} h={16} /><Sk w={72} h={26} /></div><Sk w="100%" h={180} r={12} mt={6} /></div>);
const SkPanel = () => (<div className="dcard"><div className="dcard-head"><Sk w={150} h={16} /><Sk w={56} h={14} /></div>{[0, 1, 2, 3].map((i) => (<div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '11px 0' }}><Sk w={38} h={38} r={19} /><div style={{ flex: 1 }}><Sk w="70%" h={13} /><Sk w="45%" h={11} mt={6} /></div><Sk w={50} h={20} r={10} /></div>))}</div>);

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const first = (user?.fullName || user?.username || 'there').split(' ')[0];

  const [ready, setReady] = useState({ stats: false, charts: false, panels: false });
  const [statD, setStatD] = useState<any>({});
  const [chartD, setChartD] = useState<any>({ line: { a: [0, 0, 0, 0, 0, 0, 0], b: [0, 0, 0, 0, 0, 0, 0] }, donut: [], center: 0, bars: [] });
  const [panelD, setPanelD] = useState<any>({ recent: [], appts: [], journeys: [] });

  const loadAll = () => {
    const ok = (fb: any) => (e: any) => fb; // swallow errors with fallback
    // group 1 — stat tiles
    Promise.all([
      api.get<any>('/patients', { params: { size: 1 } }).catch(ok({ data: { totalElements: 0 } })),
      api.get<any>('/requests/stats').catch(ok({ data: {} })),
      api.get<any>('/cases/dashboard').catch(ok({ data: {} })),
      api.get<any>('/live/active-journeys').catch(ok({ data: { journeys: [] } })),
    ]).then(([p, rs, cd, lj]) => {
      const req = rs.data || {}; const cs = cd.data || {}; const jr = lj.data?.journeys || [];
      setStatD({ patients: p.data?.totalElements ?? 0, newReq: req.newRequests ?? 0, pending: (req.newRequests ?? 0) + (req.underReview ?? 0) + (req.triage ?? 0), active: jr.length, cases: (cs.openCases ?? 0) + (cs.inProgressCases ?? 0), totalCases: cs.totalCases ?? 0 });
      setReady((r) => ({ ...r, stats: true }));
    });
    // group 2 — charts
    Promise.all([
      api.get<any>('/requests', { params: { size: 200 } }).catch(ok({ data: { content: [] } })),
      api.get<any>('/cases', { params: { size: 200 } }).catch(ok({ data: { content: [] } })),
      api.get<any>('/cases/dashboard').catch(ok({ data: {} })),
    ]).then(([rq, cs, cd]) => {
      const reqs = rq.data?.content || []; const cases = cs.data?.content || []; const d = cd.data || {};
      const aa = [0, 0, 0, 0, 0, 0, 0], bb = [0, 0, 0, 0, 0, 0, 0];
      const wd = (iso: string) => { const g = new Date(iso).getDay(); return (g + 6) % 7; }; // Mon=0
      reqs.forEach((r: any) => { if (r.createdAt) aa[wd(r.createdAt)]++; });
      cases.forEach((c: any) => { if (c.createdAt) bb[wd(c.createdAt)]++; });
      const svc: Record<string, number> = {};
      reqs.forEach((r: any) => (r.services || []).forEach((s: string) => { svc[s] = (svc[s] || 0) + 1; }));
      const bars = Object.entries(svc).sort((a: any, b: any) => b[1] - a[1]).slice(0, 5).map(([k, v], i) => ({ label: titleCase(k), value: v as number, color: SERVICE_COLORS[k] || palette[i % palette.length] }));
      const donut = [
        { label: 'Open', value: d.openCases ?? 0, color: '#2f9e8f' },
        { label: 'In Progress', value: d.inProgressCases ?? 0, color: '#5b9bd5' },
        { label: 'On Hold', value: d.onHoldCases ?? 0, color: '#e8955a' },
        { label: 'Closed', value: d.closedCases ?? 0, color: '#c2c6cd' },
      ];
      setChartD({ line: { a: aa, b: bb }, donut, center: d.totalCases ?? 0, bars });
      setReady((r) => ({ ...r, charts: true }));
    });
    // group 3 — panels
    Promise.all([
      api.get<any>('/requests', { params: { size: 5 } }).catch(ok({ data: { content: [] } })),
      api.get<any>('/cases', { params: { size: 5 } }).catch(ok({ data: { content: [] } })),
      api.get<any>('/live/active-journeys').catch(ok({ data: { journeys: [] } })),
    ]).then(([rq, cs, lj]) => {
      setPanelD({ recent: rq.data?.content || [], appts: cs.data?.content || [], journeys: lj.data?.journeys || [] });
      setReady((r) => ({ ...r, panels: true }));
    });
  };

  useEffect(loadAll, []);
  // refresh when live events stream in
  useLive((name) => { if (name === 'event' || name === 'request' || name === 'location') loadAll(); });

  const tiles = useMemo(() => [
    { t: 'green', n: statD.patients ?? 0, l: 'Total Patients', sub: 'registered in system', img: '/tile-patients.jpeg', to: '/patients', icon: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 20a7 7 0 0 1 14 0' },
    { t: 'blue', n: statD.newReq ?? 0, l: 'New Requests', sub: `${statD.pending ?? 0} awaiting triage`, img: '/tile-requests.jpeg', to: '/requests', icon: 'M8 3h6l4 4v13H6V4a1 1 0 0 1 1-1Zm6 0v4h4M8 12h8M8 16h5' },
    { t: 'orange', n: statD.active ?? 0, l: 'Active Transports', sub: 'en route right now', img: '/tile-transports.jpeg', to: '/live', icon: 'M3 7h9v9H2v-5l1-4Zm9 3h4l3 3v3h-7v-6ZM6 19a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Zm9 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z' },
    { t: 'purple', n: statD.cases ?? 0, l: 'Active Cases', sub: `${statD.totalCases ?? 0} total`, img: '/tile-care.jpeg', to: '/cases', icon: 'M12 21s-7-4.35-9.5-8.5C1 9.5 2.5 5.5 6.5 5.5c2 0 3 1 3.5 2 .5-1 1.5-2 3.5-2 4 0 5.5 4 4 7M3 13h4l1.5-3 2 5 1.5-2h3' },
  ], [statD]);

  const journey = panelD.journeys?.[0];

  return (
    <div>
      <div className="dash-welcome">
        <div>
          <h1>Welcome back, {first}!</h1>
          <p>Here's what's happening with your patient care operations today.</p>
        </div>
        <div className="dash-banner" />
      </div>

      <div className="dash-stats">
        {ready.stats ? tiles.map((s) => (
          <button key={s.l} className={`stat-tile t-${s.t}`} onClick={() => navigate(s.to)} style={{ cursor: 'pointer', textAlign: 'left', border: 0 }}>
            <div className="ill" style={{ backgroundImage: `url(${s.img})` }} />
            <div className="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d={s.icon} /></svg></div>
            <div className="num">{s.n}</div>
            <div className="lbl">{s.l}</div>
            <div className="trend" style={{ color: '#8a9893', fontWeight: 500 }}>{s.sub}</div>
          </button>
        )) : [0, 1, 2, 3].map((i) => <SkTile key={i} />)}
      </div>

      <div className="dash-row charts">
        {!ready.charts ? (<><SkChart /><SkChart /><SkChart /></>) : (<>
          <div className="dcard">
            <div className="dcard-head"><h3>Weekly Overview</h3><span className="dcard-pill">This Week</span></div>
            <LineChart a={chartD.line.a} b={chartD.line.b} />
            <div className="line-legend">
              <span className="row"><span className="dot" style={{ background: '#2f9e8f' }} /> Requests</span>
              <span className="row"><span className="dot" style={{ background: '#7cc47a' }} /> Cases</span>
            </div>
          </div>
          <div className="dcard">
            <div className="dcard-head"><h3>Case Status</h3><span className="dcard-pill">Live</span></div>
            <Donut segs={chartD.donut} center={chartD.center} label="Cases" />
          </div>
          <div className="dcard">
            <div className="dcard-head"><h3>Requests by Service</h3><span className="dcard-pill">All time</span></div>
            <BarChart bars={chartD.bars} />
          </div>
        </>)}
      </div>

      <div className="dash-row panels">
        {!ready.panels ? (<><SkPanel /><SkPanel /><SkPanel /></>) : (<>
          <div className="dcard">
            <div className="dcard-head"><h3>Live Transport Tracking</h3><span className="dcard-link" onClick={() => navigate('/live')}>View All →</span></div>
            {journey ? (
              <div className="track-info" style={{ position: 'static', boxShadow: 'none', marginTop: 6 }}>
                <span className="ti-ic"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M3 7h9v9H2v-5l1-4Z" /><circle cx="6" cy="17.5" r="1.5" /><circle cx="15" cy="17.5" r="1.5" /></svg></span>
                <div className="ti-meta"><b>{journey.caseNumber} · {journey.agentName}</b><span>{journey.patientName} · {String(journey.status).replace(/_/g, ' ')}</span></div>
                <span className="ti-chip">{String(journey.status).replace(/_/g, ' ')}</span>
              </div>
            ) : <div style={{ height: 120, display: 'grid', placeItems: 'center', color: '#9aa49f', fontSize: 13, textAlign: 'center' }}>No active transports right now.<br />New journeys appear here live.</div>}
          </div>

          <div className="dcard">
            <div className="dcard-head"><h3>Recent Requests</h3><span className="dcard-link" onClick={() => navigate('/requests')}>View All →</span></div>
            <div className="req-list">
              {panelD.recent.length === 0 && <div style={{ padding: '24px 0', textAlign: 'center', color: '#9aa49f', fontSize: 13 }}>No requests yet.</div>}
              {panelD.recent.map((r: any, i: number) => (
                <div className="req-row" key={r.id} style={{ cursor: 'pointer' }} onClick={() => navigate(r.caseId ? `/cases/${r.caseId}` : '/requests')}>
                  <div className="rav" style={{ background: AV[i % AV.length] }}>{(r.patientName || '?').split(' ').map((w: string) => w[0]).slice(0, 2).join('').toUpperCase()}</div>
                  <div className="meta"><b>{r.patientName}</b><span>{(r.services || []).map(titleCase).join(', ')}</span></div>
                  <div className="rt"><span className={`chip ${statusChip(r.status)}`}>{titleCase(r.status)}</span></div>
                </div>
              ))}
            </div>
          </div>

          <div className="dcard">
            <div className="dcard-head"><h3>Recent Cases</h3><span className="dcard-link" onClick={() => navigate('/cases')}>View All →</span></div>
            <div className="appt-list">
              {panelD.appts.length === 0 && <div style={{ padding: '24px 0', textAlign: 'center', color: '#9aa49f', fontSize: 13 }}>No cases yet.</div>}
              {panelD.appts.map((a: any) => { const dt = a.createdAt ? new Date(a.createdAt) : null; return (
                <div className="appt-row" key={a.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/cases/${a.id}`)}>
                  <div className="date" style={{ background: '#e9f6f0', color: '#12876f' }}><b>{dt ? dt.getDate() : '—'}</b><span>{dt ? MONTHS[dt.getMonth()] : ''}</span></div>
                  <div className="meta"><b>{a.patientName}</b><span>{titleCase(a.title) || a.caseNumber}</span></div>
                  <div className="at"><span className={`chip ${statusChip(a.status)}`}>{titleCase(a.status)}</span></div>
                </div>
              ); })}
            </div>
          </div>
        </>)}
      </div>
    </div>
  );
}
