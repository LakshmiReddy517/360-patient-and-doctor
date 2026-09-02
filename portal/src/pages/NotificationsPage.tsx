import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import { useLive } from '../api/live';
import type { NotifCategory, NotifChannel, Notification, NotificationPreference, NotificationStats, Page } from '../types';

const CHANNELS: NotifChannel[] = ['WHATSAPP', 'SMS', 'EMAIL', 'PUSH', 'INTERNAL'];
const CATEGORIES: NotifCategory[] = ['APPOINTMENT', 'PAYMENT', 'PICKUP', 'MEDICAL', 'MEDICATION', 'EMERGENCY', 'MARKETING', 'GENERAL'];

const STATUS_BADGE: Record<string, string> = {
  DELIVERED: 'badge-progress', READ: 'badge-progress', SENT: 'badge-open',
  QUEUED: 'badge-hold', FAILED: 'badge-emergency', SUPPRESSED: 'badge-closed',
};

export default function NotificationsPage() {
  const [rows, setRows] = useState<Notification[]>([]);
  const [stats, setStats] = useState<NotificationStats | null>(null);
  const [channel, setChannel] = useState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState('');
  const [showSend, setShowSend] = useState(false);
  const [showPrefs, setShowPrefs] = useState(false);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      api.get<Page<Notification>>('/notifications', { params: { channel: channel || undefined, category: category || undefined, status: status || undefined, size: 50 } }),
      api.get<NotificationStats>('/notifications/stats'),
    ]).then(([n, s]) => { setRows(n.data.content); setStats(s.data); }).finally(() => setLoading(false));
  }, [channel, category, status]);
  useEffect(() => { load(); }, [load]);

  // Dynamic: new notifications appear live.
  useLive((name) => { if (name === 'notification') load(); });

  return (
    <div>
      <div className="page-head">
        <h2>Notifications</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn" onClick={() => setShowPrefs(true)}>Preferences</button>
          <button className="btn btn-primary" onClick={() => setShowSend(true)}>+ Send</button>
        </div>
      </div>

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Total</div><div className="value">{stats?.total}</div></div>
        <div className="card stat"><div className="label">Delivered</div><div className="value primary">{stats?.delivered}</div></div>
        <div className="card stat"><div className="label">Failed</div><div className="value danger">{stats?.failed}</div></div>
        <div className="card stat"><div className="label">Suppressed</div><div className="value">{stats?.suppressed}</div></div>
      </div>

      <div className="toolbar">
        <select className="input" style={{ maxWidth: 160 }} value={channel} onChange={(e) => setChannel(e.target.value)}>
          <option value="">All channels</option>{CHANNELS.map((c) => <option key={c}>{c}</option>)}
        </select>
        <select className="input" style={{ maxWidth: 170 }} value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All categories</option>{CATEGORIES.map((c) => <option key={c}>{c}</option>)}
        </select>
        <select className="input" style={{ maxWidth: 160 }} value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>{['DELIVERED', 'SUPPRESSED', 'FAILED', 'READ', 'SENT'].map((c) => <option key={c}>{c}</option>)}
        </select>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Channel</th><th>Category</th><th>Recipient</th><th>Subject</th><th>Status</th><th>When</th></tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={6} className="spinner">Loading…</td></tr>
                : rows.length === 0 ? <tr><td colSpan={6} className="spinner">No notifications</td></tr>
                : rows.map((n) => (
                  <tr key={n.id}>
                    <td><span className="badge badge-normal">{n.channel}</span></td>
                    <td className={n.category === 'EMERGENCY' ? '' : 'muted'}>{n.category === 'EMERGENCY' ? <span className="badge badge-emergency">EMERGENCY</span> : n.category}</td>
                    <td>{n.recipientName || '—'}<div className="muted" style={{ fontSize: 11 }}>{n.recipientAddress}</div></td>
                    <td className="muted">{n.subject}</td>
                    <td><span className={'badge ' + (STATUS_BADGE[n.status] || 'badge-normal')} title={n.errorMessage || ''}>{n.status}</span></td>
                    <td className="muted">{new Date(n.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {showSend && <SendModal channels={CHANNELS} categories={CATEGORIES} onClose={() => setShowSend(false)} onSent={() => { setShowSend(false); load(); }} />}
      {showPrefs && <PrefsModal channels={CHANNELS} categories={CATEGORIES} onClose={() => setShowPrefs(false)} />}
    </div>
  );
}

const overlay: React.CSSProperties = { position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 };

function SendModal({ channels, categories, onClose, onSent }:
  { channels: NotifChannel[]; categories: NotifCategory[]; onClose: () => void; onSent: () => void }) {
  const [recipientUserId, setUserId] = useState('');
  const [recipientName, setName] = useState('');
  const [recipientAddress, setAddress] = useState('');
  const [channel, setChannel] = useState<NotifChannel>('SMS');
  const [category, setCategory] = useState<NotifCategory>('GENERAL');
  const [subject, setSubject] = useState('');
  const [body, setBody] = useState('');
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function send() {
    setSaving(true); setError(''); setResult(null);
    try {
      const { data } = await api.post('/notifications', {
        recipientUserId: recipientUserId ? Number(recipientUserId) : null,
        recipientName, recipientAddress, channel, category, subject, body,
      });
      setResult(`${data.status}${data.errorMessage ? ' — ' + data.errorMessage : ''}`);
      if (data.status !== 'SUPPRESSED') setTimeout(onSent, 800);
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 460, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>Send Notification</h3>
        {error && <div className="error-text">{error}</div>}
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Channel</label><select className="input" value={channel} onChange={(e) => setChannel(e.target.value as NotifChannel)}>{channels.map((c) => <option key={c}>{c}</option>)}</select></div>
          <div className="field" style={{ flex: 1 }}><label>Category</label><select className="input" value={category} onChange={(e) => setCategory(e.target.value as NotifCategory)}>{categories.map((c) => <option key={c}>{c}</option>)}</select></div>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Recipient user id (optional)</label><input className="input" value={recipientUserId} onChange={(e) => setUserId(e.target.value)} placeholder="e.g. 5 (patient)" /></div>
          <div className="field" style={{ flex: 1 }}><label>Name</label><input className="input" value={recipientName} onChange={(e) => setName(e.target.value)} /></div>
        </div>
        <div className="field"><label>Address (mobile/email)</label><input className="input" value={recipientAddress} onChange={(e) => setAddress(e.target.value)} /></div>
        <div className="field"><label>Subject</label><input className="input" value={subject} onChange={(e) => setSubject(e.target.value)} /></div>
        <div className="field"><label>Body</label><textarea className="input" rows={2} value={body} onChange={(e) => setBody(e.target.value)} /></div>
        {result && <div style={{ fontWeight: 600, marginBottom: 10, color: result.startsWith('SUPPRESSED') ? 'var(--danger)' : 'var(--success)' }}>{result}</div>}
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Close</button>
          <button className="btn btn-primary" onClick={send} disabled={saving || !subject}>{saving ? 'Sending…' : 'Send'}</button>
        </div>
      </div>
    </div>
  );
}

function PrefsModal({ channels, categories, onClose }:
  { channels: NotifChannel[]; categories: NotifCategory[]; onClose: () => void }) {
  const [userId, setUserId] = useState('5');
  const [prefs, setPrefs] = useState<NotificationPreference[]>([]);
  const [loaded, setLoaded] = useState(false);

  function load() {
    api.get<NotificationPreference[]>(`/notifications/preferences/${userId}`).then((r) => {
      const byCat = new Map(r.data.map((p) => [p.category, p]));
      setPrefs(categories.map((c) => byCat.get(c) || { userId: Number(userId), category: c, enabled: c !== 'MARKETING', channels: [] }));
      setLoaded(true);
    });
  }

  async function save(p: NotificationPreference) {
    await api.put('/notifications/preferences', { userId: Number(userId), category: p.category, enabled: p.enabled, channels: p.channels });
    load();
  }

  function toggleChannel(p: NotificationPreference, ch: NotifChannel) {
    const channelsNew = p.channels.includes(ch) ? p.channels.filter((x) => x !== ch) : [...p.channels, ch];
    save({ ...p, channels: channelsNew });
  }

  return (
    <div style={overlay} onClick={onClose}>
      <div className="card card-pad" style={{ width: 620, maxWidth: '95vw', maxHeight: '90vh', overflow: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 8 }}>Notification Preferences</h3>
        <div className="muted" style={{ fontSize: 12, marginBottom: 12 }}>Per-user channel/category consent. EMERGENCY always delivers; MARKETING requires opt-in.</div>
        <div className="toolbar">
          <input className="input" style={{ maxWidth: 160 }} value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="User id" />
          <button className="btn" onClick={load}>Load</button>
        </div>
        {loaded && (
          <div className="table-wrap">
            <table className="data">
              <thead><tr><th>Category</th><th>Enabled</th>{channels.map((c) => <th key={c} style={{ fontSize: 10 }}>{c}</th>)}</tr></thead>
              <tbody>
                {prefs.map((p) => (
                  <tr key={p.category}>
                    <td style={{ fontWeight: 600 }}>{p.category}</td>
                    <td><input type="checkbox" checked={p.enabled} onChange={() => save({ ...p, enabled: !p.enabled })} /></td>
                    {channels.map((c) => <td key={c}><input type="checkbox" checked={p.channels.includes(c)} onChange={() => toggleChannel(p, c)} /></td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 14 }}>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
