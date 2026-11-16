import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

type Item = { id: string; title: string; sub?: string; group: string; icon: string; to: string };

/* all pages — searched when the user types */
const PAGES: { label: string; to: string; icon: string }[] = [
  { label: 'Dashboard', to: '/', icon: 'M11.3 3.3a1 1 0 0 1 1.4 0l8 7.2M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9' },
  { label: 'Patients', to: '/patients', icon: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 20a7 7 0 0 1 14 0' },
  { label: 'Service Requests', to: '/requests', icon: 'M8 3h6l4 4v14H6V4a1 1 0 0 1 1-1Zm6 0v4h4M8 12h8M8 16h8' },
  { label: 'Cases', to: '/cases', icon: 'M4 7h16v13H4zM8 7V4h8v3' },
  { label: 'Live Operations', to: '/live', icon: 'M12 2a7 7 0 0 0-7 7c0 5 7 13 7 13s7-8 7-13a7 7 0 0 0-7-7Zm0 9.5A2.5 2.5 0 1 1 12 6a2.5 2.5 0 0 1 0 5.5Z' },
  { label: 'Agents & Dispatch', to: '/agents', icon: 'M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm6 0a3 3 0 1 0 0-6M3 20a6 6 0 0 1 12 0' },
  { label: 'Ambulance Fleet', to: '/ambulances', icon: 'M3 7h9v9H2v-5zM12 10h4l3 3v3h-7z' },
  { label: 'Hospitals', to: '/hospitals', icon: 'M6 22V4h12v18M10 8h4M12 6v4' },
  { label: 'Doctors', to: '/doctors', icon: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 20a7 7 0 0 1 14 0' },
  { label: 'Caretakers', to: '/caretakers', icon: 'M12 21s-7-4.3-9.5-8.5C1 9.5 2.5 5.5 6.5 5.5c2 0 3 1 3.5 2 .5-1 1.5-2 3.5-2 4 0 5.5 4 4 7' },
  { label: 'Equipment Inventory', to: '/inventory', icon: 'M4 7l8-4 8 4-8 4-8-4Zm0 5l8 4 8-4M4 17l8 4 8-4' },
  { label: 'Packages & Pricing', to: '/pricing', icon: 'M3 12l9-9 9 9-9 9zM12 7v10' },
  { label: 'Reports & Analytics', to: '/reports', icon: 'M4 20V10M10 20V4M16 20v-6M22 20H2' },
  { label: 'Notifications', to: '/notifications', icon: 'M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9' },
  { label: 'Settlements', to: '/settlements', icon: 'M3 6h18v12H3zM3 10h18' },
  { label: 'Complaints', to: '/complaints', icon: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z' },
];

/* curated bento — a LIMITED set of Windows-tile boxes shown by default */
const FEATURED = [
  { area: 'm-dash', label: 'Dashboard', sub: 'Overview', to: '/', bg: 'linear-gradient(135deg,#13877a,#0f766e)', icon: PAGES[0].icon },
  { area: 'm-live', label: 'Live Operations', sub: 'Track journeys in real time', to: '/live', bg: 'linear-gradient(145deg,#12776b,#0b4a44)', icon: PAGES[4].icon, feature: true },
  { area: 'm-pat', label: 'Patients', sub: '', to: '/patients', bg: 'linear-gradient(135deg,#3f9a83,#2f8f77)', icon: PAGES[1].icon },
  { area: 'm-req', label: 'Requests', sub: '', to: '/requests', bg: 'linear-gradient(135deg,#4f8fcf,#3f7cc0)', icon: PAGES[2].icon },
  { area: 'm-case', label: 'Cases', sub: '', to: '/cases', bg: 'linear-gradient(135deg,#e0934f,#d17f3c)', icon: PAGES[3].icon },
  { area: 'm-ag', label: 'Agents', sub: '', to: '/agents', bg: 'linear-gradient(135deg,#917bd0,#7c66c3)', icon: PAGES[5].icon },
  { area: 'm-rep', label: 'Reports & Analytics', sub: 'Operational insights', to: '/reports', bg: 'linear-gradient(135deg,#65757e,#55656e)', icon: PAGES[12].icon },
];

export default function CommandPalette({ open, onClose }: { open: boolean; onClose: () => void }) {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [sel, setSel] = useState(0);
  const [remote, setRemote] = useState<Item[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => { if (open) { setQ(''); setSel(0); setRemote([]); setTimeout(() => inputRef.current?.focus(), 30); } }, [open]);

  useEffect(() => {
    if (!open || q.trim().length < 2) { setRemote([]); return; }
    const term = q.trim();
    const t = setTimeout(async () => {
      const out: Item[] = [];
      try {
        const p = await api.get<any>('/patients', { params: { q: term, size: 6 } });
        (p.data.content || []).forEach((x: any) => out.push({ id: 'p' + x.id, title: x.fullName, sub: [x.mobile, x.city].filter(Boolean).join(' · '), group: 'Patients', icon: PAGES[1].icon, to: `/patients/${x.id}` }));
      } catch { /* ignore */ }
      try {
        const r = await api.get<any>('/requests', { params: { q: term, size: 6 } });
        const hz = (s: string) => (s || '').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (m) => m.toUpperCase());
        (r.data.content || []).forEach((x: any) => out.push({ id: 'r' + x.id, title: `${x.patientName} — request #${x.id}`, sub: `${[...(x.services || [])].map(hz).join(', ')} · ${hz(x.status)}`, group: 'Requests', icon: PAGES[2].icon, to: x.caseId ? `/cases/${x.caseId}` : '/requests' }));
      } catch { /* ignore */ }
      setRemote(out); setSel(0);
    }, 180);
    return () => clearTimeout(t);
  }, [q, open]);

  const searching = q.trim().length > 0;
  const pageItems: Item[] = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) return [];
    return PAGES.filter((p) => p.label.toLowerCase().includes(term))
      .map((p) => ({ id: 'nav' + p.to, title: p.label, group: 'Navigate', icon: p.icon, to: p.to }));
  }, [q]);

  const featuredItems: Item[] = useMemo(() => FEATURED.map((f) => ({ id: 'f' + f.to, title: f.label, sub: f.sub, group: 'Quick', icon: f.icon, to: f.to })), []);
  const all = useMemo(() => (searching ? [...pageItems, ...remote] : featuredItems), [searching, pageItems, remote, featuredItems]);
  const groups = useMemo(() => {
    const m = new Map<string, Item[]>();
    [...pageItems, ...remote].forEach((it) => { (m.get(it.group) || m.set(it.group, []).get(it.group)!).push(it); });
    return Array.from(m.entries());
  }, [pageItems, remote]);

  const go = useCallback((it?: Item) => { if (!it) return; onClose(); navigate(it.to); }, [navigate, onClose]);
  const onKey = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); setSel((s) => Math.min(s + 1, all.length - 1)); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setSel((s) => Math.max(s - 1, 0)); }
    else if (e.key === 'Enter') { e.preventDefault(); go(all[sel]); }
    else if (e.key === 'Escape') { e.preventDefault(); onClose(); }
  };

  if (!open) return null;
  let idx = -1;
  return (
    <div className="cmdk-overlay" onMouseDown={onClose}>
      <div className="cmdk" onMouseDown={(e) => e.stopPropagation()} onKeyDown={onKey}>
        <div className="cmdk-input">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round"><circle cx="11" cy="11" r="7" /><path d="m20 20-3-3" /></svg>
          <input ref={inputRef} value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search patients, requests, agents, pages…" />
          <kbd className="cmdk-esc">ESC</kbd>
        </div>

        <div className="cmdk-results">
          {!searching && (
            <>
              <div className="cmdk-group-label" style={{ padding: '10px 8px 10px' }}>Quick access</div>
              <div className="cmdk-metro">
                {FEATURED.map((f, i) => {
                  const active = i === sel;
                  return (
                    <button key={f.to} className={`cmdk-mtile ${f.area}${f.feature ? ' feature' : ''}${active ? ' active' : ''}`}
                      style={{ background: f.bg }} onMouseEnter={() => setSel(i)} onClick={() => go(featuredItems[i])}>
                      {f.feature && <span className="m-live-dot" aria-hidden="true" />}
                      <span className="m-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round"><path d={f.icon} /></svg></span>
                      <span className="m-tt"><b>{f.label}</b>{f.sub && <span>{f.sub}</span>}</span>
                    </button>
                  );
                })}
              </div>
            </>
          )}

          {searching && (
            <div className="cmdk-bento">
              {all.length === 0 && <div className="cmdk-empty">No results for “{q}”</div>}
              {groups.map(([label, items]) => (
                <div className="cmdk-bgroup" key={label}>
                  <div className="cmdk-group-label">{label}</div>
                  <div className="cmdk-grid">
                    {items.map((it) => {
                      idx++; const active = idx === sel; const myIdx = idx;
                      return (
                        <button key={it.id} className={'cmdk-tile' + (active ? ' active' : '')} onMouseEnter={() => setSel(myIdx)} onClick={() => go(it)}>
                          <span className="cmdk-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"><path d={it.icon} /></svg></span>
                          <span className="cmdk-tt"><b>{it.title}</b>{it.sub && <span>{it.sub}</span>}</span>
                          <span className="cmdk-enter">↵</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="cmdk-foot">
          <span><kbd>↑</kbd><kbd>↓</kbd> navigate</span>
          <span><kbd>↵</kbd> open</span>
          <span><kbd>esc</kbd> close</span>
          <span className="cmdk-brand">360° Command</span>
        </div>
      </div>
    </div>
  );
}
