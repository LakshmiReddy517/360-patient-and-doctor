import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/* Icons (filled glyphs matching the design) */
const I = {
  dash: 'M11.3 3.26a1 1 0 0 1 1.4 0l8.05 7.67A1 1 0 0 1 20 12.66V20a1 1 0 0 1-1 1h-4.5v-5.5h-5V21H5a1 1 0 0 1-1-1v-7.34a1 1 0 0 1 .31-.73z',
  patients: 'M8.5 11a3.25 3.25 0 1 0 0-6.5 3.25 3.25 0 0 0 0 6.5Zm7 0a3.25 3.25 0 1 0 0-6.5 3.25 3.25 0 0 0 0 6.5Zm-7 1.5c-3.2 0-5.5 1.8-5.5 4V18a1 1 0 0 0 1 1h5.9a4 4 0 0 1-.4-1.5v-1c0-.9.2-1.7.6-2.4-.5-.1-1-.1-1.6-.1Zm7 0c-.5 0-1 .05-1.5.13.4.7.6 1.5.6 2.37v1c0 .54-.14 1.04-.4 1.5H20a1 1 0 0 0 1-1v-.5c0-2.2-2.3-4-5.5-4Z',
  requests: 'M6 2a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9h-5a2 2 0 0 1-2-2V2H6Zm8 .4V7a1 1 0 0 0 1 1h4.6L14 2.4ZM8 12h8v1.6H8V12Zm0 3.4h8V17H8v-1.6Zm0-6.8h2.5v1.6H8V8.6Z',
  transport: 'M2 8a1 1 0 0 1 1-1h9a1 1 0 0 1 1 1v2h2.2a1 1 0 0 1 .8.4l2.8 3.7a1 1 0 0 1 .2.6V16a1 1 0 0 1-1 1h-.6a2.5 2.5 0 0 1-4.8 0H9.4a2.5 2.5 0 0 1-4.8 0H3a1 1 0 0 1-1-1V8Zm13 3.5V13h3.3l-1.3-1.7a.5.5 0 0 0-.4-.2H15a.5.5 0 0 0-.5.4ZM6.5 15a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3Zm9 0a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3Zm-8-7v1.5H6V9h-.5V8H7.5V6.5H9V8H8.5v.5H7.5Z',
  agents: 'M8.5 11a3.25 3.25 0 1 0 0-6.5 3.25 3.25 0 0 0 0 6.5Zm7 0a3.25 3.25 0 1 0 0-6.5 3.25 3.25 0 0 0 0 6.5Zm-7 1.5c-3.2 0-5.5 1.8-5.5 4V18a1 1 0 0 0 1 1h5.9a4 4 0 0 1-.4-1.5v-1c0-.9.2-1.7.6-2.4-.5-.1-1-.1-1.6-.1Zm7 0c-.5 0-1 .05-1.5.13.4.7.6 1.5.6 2.37v1c0 .54-.14 1.04-.4 1.5H20a1 1 0 0 0 1-1v-.5c0-2.2-2.3-4-5.5-4Z',
  care: 'M12 2 4 5v6c0 5 3.4 8.5 8 10 4.6-1.5 8-5 8-10V5l-8-3Zm3.7 6.3 1.4 1.4-5.6 5.6-3.6-3.6 1.4-1.4 2.2 2.2 4.2-4.2Z',
  appts: 'M7 2a1 1 0 0 1 1 1v1h8V3a1 1 0 1 1 2 0v1h1a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h1V3a1 1 0 0 1 1-1Zm13 7H4v10a0 0 0 0 0 0 0h16V9ZM7 11h3v3H7v-3Z',
  reports: 'M4 13h3v7H4a0 0 0 0 1 0 0v-7Zm5.5-6h3v13h-3V7Zm5.5 3h3v10h-3V10Z',
  settings: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Zm0 2a2 2 0 1 1 0 4 2 2 0 0 1 0-4Zm7.4 2c0-.5-.05-1-.13-1.47l1.9-1.48-2-3.46-2.24.95a7 7 0 0 0-2.54-1.47L14 2.5h-4l-.4 2.6a7 7 0 0 0-2.54 1.47L4.83 5.6l-2 3.46 1.9 1.48a7.2 7.2 0 0 0 0 2.94l-1.9 1.48 2 3.46 2.24-.95a7 7 0 0 0 2.54 1.47L10 21.5h4l.4-2.56a7 7 0 0 0 2.54-1.47l2.24.95 2-3.46-1.9-1.48c.08-.47.13-.97.13-1.47Z',
};

type Item = { to: string; label: string; icon: string; sub?: [string, string][] };

/* Exactly the design's 9 items; extra portal pages live in floating hover submenus. */
const NAV: Item[] = [
  { to: '/', label: 'Dashboard', icon: I.dash },
  { to: '/patients', label: 'Patient Management', icon: I.patients, sub: [['/patients', 'Patients'], ['/consent', 'Consent'], ['/service-advisor', 'Service Advisor']] },
  { to: '/requests', label: 'Requests', icon: I.requests, sub: [['/requests', 'Service Requests'], ['/cases', 'Cases']] },
  { to: '/ambulances', label: 'Transport & Ambulance', icon: I.transport, sub: [['/ambulances', 'Ambulance Fleet'], ['/live', 'Live Operations']] },
  { to: '/agents', label: 'Agents & Staff', icon: I.agents, sub: [['/agents', 'Agents & Dispatch'], ['/doctors', 'Doctors']] },
  { to: '/caretakers', label: 'Care Coordination', icon: I.care, sub: [['/caretakers', 'Caretakers'], ['/accommodations', 'Accommodation'], ['/inventory', 'Equipment Inventory'], ['/hospitals', 'Hospitals']] },
  { to: '/cases', label: 'Appointments', icon: I.appts },
  { to: '/reports', label: 'Reports & Analytics', icon: I.reports },
  { to: '/notifications', label: 'Settings', icon: I.settings, sub: [['/notifications', 'Notifications'], ['/pricing', 'Packages & Pricing'], ['/cash', 'Cash Reconciliation'], ['/settlements', 'Settlements'], ['/complaints', 'Complaints'], ['/incidents', 'Incidents']] },
];

function Icon({ d }: { d: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor">
      <path d={d} fillRule="evenodd" clipRule="evenodd" />
    </svg>
  );
}

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const name = user?.fullName || user?.username || 'User';
  const initials = name.split(' ').map((s) => s[0]).slice(0, 2).join('').toUpperCase();

  return (
    <div className="ax-shell">
      <aside className="ax-side">
        <div className="ax-logo">
          <img src="/dash-logo.jpeg" alt="360° Patient Care Management System" />
        </div>
        <nav className="ax-nav">
          {NAV.map((n) => (
            <div className="ax-nav-item" key={n.to}>
              <NavLink to={n.to} end={n.to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}>
                <Icon d={n.icon} />
                <span className="lbl">{n.label}</span>
              </NavLink>
              {n.sub && (
                <div className="ax-submenu">
                  <div className="ax-submenu-head">{n.label}</div>
                  {n.sub.map(([to, label]) => (
                    <NavLink key={to} to={to} end className={({ isActive }) => (isActive ? 'active' : '')}>{label}</NavLink>
                  ))}
                </div>
              )}
            </div>
          ))}
        </nav>
        <div className="ax-side-spacer" />
        <div className="ax-side-art" />
        <div className="ax-side-foot" onClick={() => navigate('/live')}>
          <span className="pin">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M12 21s-6-5.2-6-10a6 6 0 1 1 12 0c0 4.8-6 10-6 10Z" /><circle cx="12" cy="11" r="2" /></svg>
          </span>
          <div>Patient App · Agent App<br />Operations Portal</div>
          <span className="arrow">→</span>
        </div>
      </aside>

      <div className="ax-main">
        <header className="ax-top">
          <div className="ax-search">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><circle cx="11" cy="11" r="7" /><path d="m20 20-3-3" /></svg>
            <input placeholder="Search patients, requests, agents, locations..." />
          </div>
          <div className="ax-top-right">
            <div className="ax-bell">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" /></svg>
              <span className="dot" />
            </div>
            <div className="ax-user" onClick={logout} title="Click to log out">
              <div className="av">{initials}</div>
              <div className="nm">{name}</div>
              <svg className="cv" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6" /></svg>
            </div>
          </div>
        </header>
        <main className="ax-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
