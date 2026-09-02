import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/** Grouped navigation mirrors the blueprint's information architecture (point 58):
 *  Operations, Patient Management, Healthcare, Transport, Care Services, Finance, etc.
 *  Items marked `soon` are wired in later modules. */
/** `fin: true` marks items a FINANCE-only user may see. ADMIN / SUPER_ADMIN see everything. */
const NAV: { group: string; items: { to: string; label: string; icon: string; soon?: boolean; fin?: boolean }[] }[] = [
  {
    group: 'Operations',
    items: [
      { to: '/', label: 'Dashboard', icon: '▚', fin: true },
      { to: '/live', label: 'Live Operations', icon: '◉', fin: true },
      { to: '/requests', label: 'Service Requests', icon: '✦', fin: true },
      { to: '/cases', label: 'Cases', icon: '☰', fin: true },
      { to: '/agents', label: 'Agents & Dispatch', icon: '➤' },
    ],
  },
  {
    group: 'Patient Management',
    items: [
      { to: '/patients', label: 'Patients', icon: '☺' },
      { to: '/consent', label: 'Consent', icon: '✓', soon: true },
    ],
  },
  {
    group: 'Healthcare',
    items: [
      { to: '/hospitals', label: 'Hospitals', icon: '✚' },
    ],
  },
  {
    group: 'Transport & Care',
    items: [
      { to: '/ambulances', label: 'Ambulance Fleet', icon: '⛑' },
      { to: '/caretakers', label: 'Caretakers', icon: '♥' },
    ],
  },
  {
    group: 'Finance',
    items: [
      { to: '/pricing', label: 'Packages & Pricing', icon: '₹', fin: true },
      { to: '/settlements', label: 'Settlements', icon: '◈', fin: true },
    ],
  },
  {
    group: 'Communication',
    items: [{ to: '/notifications', label: 'Notifications', icon: '✉', fin: true }],
  },
  {
    group: 'Support',
    items: [
      { to: '/complaints', label: 'Complaints', icon: '⚑' },
      { to: '/incidents', label: 'Incidents', icon: '⚠' },
    ],
  },
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const initials = (user?.fullName || user?.username || '?')
    .split(' ').map((s) => s[0]).slice(0, 2).join('').toUpperCase();

  // Role-based menu: FINANCE (without an admin role) gets a focused, finance-relevant menu.
  const roles = user?.roles || [];
  const isAdmin = roles.some((r) => r === 'ADMIN' || r === 'SUPER_ADMIN');
  const financeOnly = !isAdmin && roles.includes('FINANCE');
  const sections = NAV
    .map((s) => ({ ...s, items: financeOnly ? s.items.filter((i) => i.fin) : s.items }))
    .filter((s) => s.items.length > 0);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          360° Patient Care
          <small>{financeOnly ? 'Finance Console' : 'Command Centre'}</small>
        </div>
        {sections.map((section) => (
          <div key={section.group}>
            <div className="nav-group">{section.group}</div>
            {section.items.map((item) =>
              item.soon ? (
                <div key={item.to} className="nav-item" style={{ opacity: 0.4, cursor: 'not-allowed' }} title="Coming in a later module">
                  <span className="nav-icon">{item.icon}</span>
                  {item.label}
                  <span style={{ marginLeft: 'auto', fontSize: 9, textTransform: 'uppercase' }}>soon</span>
                </div>
              ) : (
                <NavLink key={item.to} to={item.to} end={item.to === '/'} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}>
                  <span className="nav-icon">{item.icon}</span>
                  {item.label}
                </NavLink>
              )
            )}
          </div>
        ))}
        <div style={{ flex: 1 }} />
        <div style={{ padding: '14px 18px', fontSize: 11, color: '#64748b', borderTop: '1px solid #e4e9f2' }}>
          Developed by <strong style={{ color: '#0f766e' }}>pvalr</strong>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <h1>Command Centre</h1>
          <div className="user-chip">
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontWeight: 600, fontSize: 13 }}>{user?.fullName}</div>
              <div className="muted" style={{ fontSize: 11 }}>{user?.roles.join(', ')}</div>
            </div>
            <div className="avatar">{initials}</div>
            <button className="btn btn-sm" onClick={logout}>Logout</button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
