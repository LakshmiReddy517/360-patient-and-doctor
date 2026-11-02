import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/* ---------- small chart helpers ---------- */
function smoothPath(pts: [number, number][]) {
  if (pts.length < 2) return '';
  let d = `M ${pts[0][0]} ${pts[0][1]}`;
  for (let i = 0; i < pts.length - 1; i++) {
    const [x0, y0] = pts[i];
    const [x1, y1] = pts[i + 1];
    const cx = (x0 + x1) / 2;
    d += ` C ${cx} ${y0}, ${cx} ${y1}, ${x1} ${y1}`;
  }
  return d;
}

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const NEWREQ = [17, 22, 28, 35, 26, 42, 33];
const DONE = [13, 15, 20, 24, 19, 30, 24];

function LineChart() {
  const W = 340, H = 190, padL = 28, padR = 14, padT = 14, padB = 28;
  const max = 50;
  const x = (i: number) => padL + (i * (W - padL - padR)) / (DAYS.length - 1);
  const y = (v: number) => padT + (1 - v / max) * (H - padT - padB);
  const p1 = NEWREQ.map((v, i) => [x(i), y(v)] as [number, number]);
  const p2 = DONE.map((v, i) => [x(i), y(v)] as [number, number]);
  const area = (pts: [number, number][]) => smoothPath(pts) + ` L ${x(DAYS.length - 1)} ${H - padB} L ${x(0)} ${H - padB} Z`;
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }}>
      <defs>
        <linearGradient id="g1" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#2f9e8f" stopOpacity=".22" /><stop offset="1" stopColor="#2f9e8f" stopOpacity="0" /></linearGradient>
        <linearGradient id="g2" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#7cc47a" stopOpacity=".2" /><stop offset="1" stopColor="#7cc47a" stopOpacity="0" /></linearGradient>
      </defs>
      {[0, 10, 20, 30, 40, 50].map((v) => (
        <g key={v}>
          <line x1={padL} y1={y(v)} x2={W - padR} y2={y(v)} stroke="#eef0ec" strokeWidth="1" />
          <text x={padL - 6} y={y(v) + 3} textAnchor="end" fontSize="9" fill="#9aa39f">{v}</text>
        </g>
      ))}
      <path d={area(p2)} fill="url(#g2)" /><path d={area(p1)} fill="url(#g1)" />
      <path d={smoothPath(p2)} fill="none" stroke="#7cc47a" strokeWidth="2.4" />
      <path d={smoothPath(p1)} fill="none" stroke="#2f9e8f" strokeWidth="2.6" />
      {p1.map(([cx, cy], i) => <circle key={i} cx={cx} cy={cy} r="3" fill="#2f9e8f" />)}
      {DAYS.map((d, i) => <text key={d} x={x(i)} y={H - 9} textAnchor="middle" fontSize="10" fill="#9aa39f">{d}</text>)}
      {/* tooltip at Sat peak — white bubble like the design */}
      <g transform={`translate(${x(5)}, ${y(42) - 20})`}>
        <rect x="-26" y="-14" width="52" height="26" rx="7" fill="#fff" stroke="#e3e6e1" strokeWidth="1" />
        <text x="0" y="-2.5" textAnchor="middle" fontSize="9.5" fontWeight="700" fill="#223733">42</text>
        <text x="0" y="7.5" textAnchor="middle" fontSize="7" fill="#8a9893">Requests</text>
      </g>
    </svg>
  );
}

const DONUT = [
  { label: 'On Route', value: 6, color: '#2f9e8f' },
  { label: 'At Location', value: 3, color: '#5b9bd5' },
  { label: 'Completed', value: 18, color: '#e8955a' },
  { label: 'Cancelled', value: 2, color: '#c2c6cd' },
];
function Donut() {
  const r = 52, c = 2 * Math.PI * r, total = DONUT.reduce((s, d) => s + d.value, 0);
  let acc = 0;
  return (
    <div className="donut-wrap">
      <div className="center">
        <svg width="132" height="132" viewBox="0 0 132 132">
          <circle cx="66" cy="66" r={r} fill="none" stroke="#f0f1ee" strokeWidth="14" />
          {DONUT.map((d, i) => {
            const len = (d.value / total) * c;
            const el = <circle key={i} cx="66" cy="66" r={r} fill="none" stroke={d.color} strokeWidth="14" strokeLinecap="round" strokeDasharray={`${len} ${c - len}`} strokeDashoffset={-acc} transform="rotate(-90 66 66)" />;
            acc += len; return el;
          })}
        </svg>
        <div className="big"><div><b>12</b><span>Active</span></div></div>
      </div>
      <div className="leg flex1">
        {DONUT.map((d) => (
          <div className="row" key={d.label}><span className="sw" style={{ background: d.color }} />{d.label}<span className="ct">{d.value}</span></div>
        ))}
      </div>
    </div>
  );
}

const BARS = [
  { label: 'Medical\nAssistance', value: 78, color: '#6cc3a0' },
  { label: 'Transport', value: 52, color: '#6aa9e0' },
  { label: 'Home Care', value: 36, color: '#eca96a' },
  { label: 'Follow-up', value: 24, color: '#a99ce0' },
  { label: 'Others', value: 18, color: '#c2c6cd' },
];
function BarChart() {
  const max = 80, H = 180;
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-around', height: H, gap: 6, padding: '8px 4px 0' }}>
      {BARS.map((b) => (
        <div key={b.label} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', height: '100%', justifyContent: 'flex-end' }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: '#223733', marginBottom: 5 }}>{b.value}</div>
          <div style={{ width: '58%', maxWidth: 34, height: `${(b.value / max) * (H - 48)}px`, background: b.color, borderRadius: '7px 7px 3px 3px' }} />
          <div style={{ fontSize: 10, color: '#8a9893', marginTop: 7, textAlign: 'center', whiteSpace: 'pre-line', lineHeight: 1.15 }}>{b.label}</div>
        </div>
      ))}
    </div>
  );
}

/* ---------- static data (mirrors the design) ---------- */
const STATS = [
  { t: 'green', n: '248', l: 'Total Patients', tr: '+12%', img: '/tile-patients.jpeg', icon: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 20a7 7 0 0 1 14 0M18 8a3 3 0 0 0 0-2M20 20a5 5 0 0 0-3-4' },
  { t: 'blue', n: '36', l: 'New Requests', tr: '+8%', img: '/tile-requests.jpeg', icon: 'M8 3h6l4 4v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h3Zm6 0v4h4M8 12h8M8 16h5' },
  { t: 'orange', n: '12', l: 'Active Transports', tr: '+3%', img: '/tile-transports.jpeg', icon: 'M3 7h9v9H2v-5l1-4Zm9 3h4l3 3v3h-7v-6ZM6 19a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Zm9 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z' },
  { t: 'purple', n: '128', l: 'Ongoing Care Plans', tr: '+15%', img: '/tile-care.jpeg', icon: 'M12 21s-7-4.35-9.5-8.5C1 9.5 2.5 5.5 6.5 5.5c2 0 3 1 3.5 2 .5-1 1.5-2 3.5-2 4 0 5.5 4 4 7M3 13h4l1.5-3 2 5 1.5-2h3' },
];
const RECENT = [
  { in: 'RS', nm: 'Ramesh Singh', ty: 'Ambulance Transport', tm: '10 mins ago', st: 'On Route', cls: 'chip-route', av: '#2f9e8f' },
  { in: 'PP', nm: 'Priya Patel', ty: 'Medical Assistance', tm: '28 mins ago', st: 'Assigned', cls: 'chip-assigned', av: '#5b9bd5' },
  { in: 'AK', nm: 'Abdul Karim', ty: 'Home Care', tm: '1 hour ago', st: 'In Progress', cls: 'chip-prog', av: '#e8955a' },
  { in: 'SN', nm: 'Sneha Naik', ty: 'Follow-up Visit', tm: '2 hours ago', st: 'Completed', cls: 'chip-done', av: '#9b8fd0' },
];
const APPTS = [
  { d: '16', m: 'Oct', nm: 'Anita Sharma', ty: 'Follow-up Consultation', at: '10:00 AM', c: '#e9f6f0', cc: '#12876f' },
  { d: '16', m: 'Oct', nm: 'Transport - Care Home', ty: 'Pickup from Residence', at: '12:30 PM', c: '#e9f1fb', cc: '#3b7fd0' },
  { d: '17', m: 'Oct', nm: 'Rahul Mehta', ty: 'Home Care Visit', at: '09:00 AM', c: '#fdf1e7', cc: '#d9813f' },
  { d: '17', m: 'Oct', nm: 'Saira Begum', ty: 'Medical Review', at: '03:30 PM', c: '#f1edfb', cc: '#7a68c9' },
];

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const first = (user?.fullName || user?.username || 'there').split(' ')[0];

  return (
    <div>
      <div className="dash-welcome">
        <div>
          <h1>Welcome Back, {first}!</h1>
          <p>Here's what's happening with your patient care operations today.</p>
        </div>
        <div className="dash-banner" />
      </div>

      <div className="dash-stats">
        {STATS.map((s) => (
          <div key={s.l} className={`stat-tile t-${s.t}`}>
            <div className="ill" style={{ backgroundImage: `url(${s.img})` }} />
            <div className="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d={s.icon} /></svg></div>
            <div className="num">{s.n}</div>
            <div className="lbl">{s.l}</div>
            <div className="trend">▲ {s.tr}</div>
          </div>
        ))}
      </div>

      <div className="dash-row charts">
        <div className="dcard">
          <div className="dcard-head"><h3>Request Overview</h3><span className="dcard-pill">This Week ▾</span></div>
          <LineChart />
          <div className="line-legend">
            <span className="row"><span className="dot" style={{ background: '#2f9e8f' }} /> New Requests</span>
            <span className="row"><span className="dot" style={{ background: '#7cc47a' }} /> Completed</span>
          </div>
        </div>
        <div className="dcard">
          <div className="dcard-head"><h3>Transport Status</h3><span className="dcard-pill">Today ▾</span></div>
          <Donut />
        </div>
        <div className="dcard">
          <div className="dcard-head"><h3>Patients by Care Type</h3><span className="dcard-pill">This Month ▾</span></div>
          <BarChart />
        </div>
      </div>

      <div className="dash-row panels">
        <div className="dcard">
          <div className="dcard-head"><h3>Live Transport Tracking</h3><span className="dcard-link" onClick={() => navigate('/live')}>View All →</span></div>
          <div className="track-map" />
        </div>

        <div className="dcard">
          <div className="dcard-head"><h3>Recent Requests</h3><span className="dcard-link" onClick={() => navigate('/requests')}>View All →</span></div>
          <div className="req-list">
            {RECENT.map((r) => (
              <div className="req-row" key={r.nm}>
                <div className="rav" style={{ background: r.av }}>{r.in}</div>
                <div className="meta"><b>{r.nm}</b><span>{r.ty}</span></div>
                <div className="rt"><span className="time">{r.tm}</span><span className={`chip ${r.cls}`}>{r.st}</span></div>
              </div>
            ))}
          </div>
        </div>

        <div className="dcard">
          <div className="dcard-head"><h3>Upcoming Appointments</h3><span className="dcard-link" onClick={() => navigate('/cases')}>View All →</span></div>
          <div className="appt-list">
            {APPTS.map((a, i) => (
              <div className="appt-row" key={i}>
                <div className="date" style={{ background: a.c, color: a.cc }}><b>{a.d}</b><span>{a.m}</span></div>
                <div className="meta"><b>{a.nm}</b><span>{a.ty}</span></div>
                <div className="at"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: 4, verticalAlign: '-2px' }}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>{a.at}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
