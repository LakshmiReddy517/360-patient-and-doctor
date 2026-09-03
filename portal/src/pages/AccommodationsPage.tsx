import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Accommodation, NearbyAccommodation } from '../types';

export default function AccommodationsPage() {
  const [rows, setRows] = useState<Accommodation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [city, setCity] = useState('');

  // Nearby search.
  const [lat, setLat] = useState('12.97');
  const [lng, setLng] = useState('77.62');
  const [radius, setRadius] = useState('10');
  const [nearby, setNearby] = useState<NearbyAccommodation[]>([]);
  const [nearbyLoading, setNearbyLoading] = useState(false);

  // Add form.
  const [showAdd, setShowAdd] = useState(false);
  const [saving, setSaving] = useState(false);
  const [f, setF] = useState({
    name: '', type: 'HOTEL', addressLine: '', city: '', latitude: '', longitude: '',
    roomType: '', pricePerNight: '0', distanceToHospitalKm: '', roomsAvailable: '', contactPhone: '',
  });

  const load = useCallback(() => {
    setLoading(true);
    api.get<Accommodation[]>('/accommodations', { params: { city: city || undefined } })
      .then((r) => setRows(r.data))
      .finally(() => setLoading(false));
  }, [city]);

  useEffect(() => { load(); }, [load]);

  async function searchNearby() {
    setNearbyLoading(true); setError('');
    try {
      const r = await api.get<NearbyAccommodation[]>('/accommodations/nearby', {
        params: { latitude: Number(lat), longitude: Number(lng), radiusKm: Number(radius) },
      });
      setNearby(r.data);
    } catch (e) { setError(apiErrorMessage(e)); } finally { setNearbyLoading(false); }
  }

  async function addAccommodation() {
    if (!f.name.trim()) { setError('Name is required'); return; }
    setSaving(true); setError('');
    try {
      await api.post<Accommodation>('/accommodations', {
        name: f.name.trim(), type: f.type || undefined, addressLine: f.addressLine || undefined, city: f.city || undefined,
        latitude: f.latitude ? Number(f.latitude) : undefined, longitude: f.longitude ? Number(f.longitude) : undefined,
        roomType: f.roomType || undefined, pricePerNight: Number(f.pricePerNight),
        distanceToHospitalKm: f.distanceToHospitalKm ? Number(f.distanceToHospitalKm) : undefined,
        available: true, roomsAvailable: f.roomsAvailable ? Number(f.roomsAvailable) : undefined,
        contactPhone: f.contactPhone || undefined, active: true,
      });
      setShowAdd(false);
      setF({ name: '', type: 'HOTEL', addressLine: '', city: '', latitude: '', longitude: '', roomType: '', pricePerNight: '0', distanceToHospitalKm: '', roomsAvailable: '', contactPhone: '' });
      load();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  const availableCount = rows.filter((r) => r.available).length;
  const minPrice = rows.length ? Math.min(...rows.map((r) => r.pricePerNight)) : 0;

  return (
    <div>
      <div className="page-head">
        <h2>Accommodations</h2>
        <button className="btn btn-primary" onClick={() => setShowAdd((s) => !s)}>{showAdd ? 'Close' : '+ Add accommodation'}</button>
      </div>
      {error && <div className="error-text">{error}</div>}

      <div className="stat-grid" style={{ marginBottom: 20 }}>
        <div className="card stat"><div className="label">Total options</div><div className="value primary">{rows.length}</div></div>
        <div className="card stat"><div className="label">Available</div><div className="value">{availableCount}</div></div>
        <div className="card stat"><div className="label">Min price ₹</div><div className="value mono">{minPrice}</div></div>
      </div>

      {showAdd && (
        <div className="card" style={{ marginBottom: 20 }}>
          <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Add accommodation</h3></div>
          <div className="card-pad" style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
            <div className="field" style={{ flex: 2, minWidth: 160 }}><label>Name</label><input className="input" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 120 }}><label>Type</label><input className="input" value={f.type} onChange={(e) => setF({ ...f, type: e.target.value })} placeholder="HOTEL / GUEST_HOUSE" /></div>
            <div className="field" style={{ flex: 2, minWidth: 160 }}><label>Address</label><input className="input" value={f.addressLine} onChange={(e) => setF({ ...f, addressLine: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 120 }}><label>City</label><input className="input" value={f.city} onChange={(e) => setF({ ...f, city: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 110 }}><label>Latitude</label><input className="input" type="number" value={f.latitude} onChange={(e) => setF({ ...f, latitude: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 110 }}><label>Longitude</label><input className="input" type="number" value={f.longitude} onChange={(e) => setF({ ...f, longitude: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 120 }}><label>Room type</label><input className="input" value={f.roomType} onChange={(e) => setF({ ...f, roomType: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 110 }}><label>Price/night ₹</label><input className="input" type="number" value={f.pricePerNight} onChange={(e) => setF({ ...f, pricePerNight: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 120 }}><label>Dist. to hospital km</label><input className="input" type="number" value={f.distanceToHospitalKm} onChange={(e) => setF({ ...f, distanceToHospitalKm: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 110 }}><label>Rooms available</label><input className="input" type="number" value={f.roomsAvailable} onChange={(e) => setF({ ...f, roomsAvailable: e.target.value })} /></div>
            <div className="field" style={{ flex: 1, minWidth: 130 }}><label>Contact phone</label><input className="input" value={f.contactPhone} onChange={(e) => setF({ ...f, contactPhone: e.target.value })} /></div>
            <button className="btn btn-primary" onClick={addAccommodation} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </div>
      )}

      <div className="toolbar" style={{ marginBottom: 14 }}>
        <input className="input" style={{ maxWidth: 300 }} placeholder="Filter by city…" value={city} onChange={(e) => setCity(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load()} />
        <button className="btn" onClick={load}>Filter</button>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Name</th><th>Type</th><th>City</th><th>Room type</th><th>Price/night</th><th>Rooms</th><th>Dist. hospital</th><th>Available</th><th>Contact</th></tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={9} className="spinner">Loading…</td></tr> : rows.length === 0 ? <tr><td colSpan={9} className="spinner">No accommodations</td></tr> : rows.map((r) => (
                <tr key={r.id}>
                  <td style={{ fontWeight: 600 }}>{r.name}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{r.addressLine || '—'}</div></td>
                  <td>{r.type ? <span className="badge badge-normal">{r.type}</span> : '—'}</td>
                  <td className="muted">{r.city || '—'}</td>
                  <td className="muted">{r.roomType || '—'}</td>
                  <td className="mono">₹{r.pricePerNight}</td>
                  <td className="mono">{r.roomsAvailable ?? '—'}</td>
                  <td className="mono">{r.distanceToHospitalKm != null ? r.distanceToHospitalKm + ' km' : '—'}</td>
                  <td>{r.available ? <span className="badge badge-progress">yes</span> : <span className="badge badge-closed">no</span>}</td>
                  <td className="muted">{r.contactPhone || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <div className="card-pad" style={{ borderBottom: '1px solid var(--border)' }}><h3>Near hospital</h3></div>
        <div className="card-pad" style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ minWidth: 120 }}><label>Latitude</label><input className="input" type="number" value={lat} onChange={(e) => setLat(e.target.value)} /></div>
          <div className="field" style={{ minWidth: 120 }}><label>Longitude</label><input className="input" type="number" value={lng} onChange={(e) => setLng(e.target.value)} /></div>
          <div className="field" style={{ minWidth: 110 }}><label>Radius km</label><input className="input" type="number" value={radius} onChange={(e) => setRadius(e.target.value)} /></div>
          <button className="btn btn-primary" onClick={searchNearby} disabled={nearbyLoading}>{nearbyLoading ? 'Searching…' : 'Search nearby'}</button>
        </div>
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Name</th><th>Type</th><th>City</th><th>Price/night</th><th>Distance</th><th>Available</th></tr></thead>
            <tbody>
              {nearbyLoading ? <tr><td colSpan={6} className="spinner">Searching…</td></tr> : nearby.length === 0 ? <tr><td colSpan={6} className="spinner">No results yet</td></tr> : nearby.map((n) => (
                <tr key={n.option.id}>
                  <td style={{ fontWeight: 600 }}>{n.option.name}</td>
                  <td>{n.option.type ? <span className="badge badge-normal">{n.option.type}</span> : '—'}</td>
                  <td className="muted">{n.option.city || '—'}</td>
                  <td className="mono">₹{n.option.pricePerNight}</td>
                  <td><span className="badge badge-open mono">{n.distanceKm.toFixed(1)} km</span></td>
                  <td>{n.option.available ? <span className="badge badge-progress">yes</span> : <span className="badge badge-closed">no</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
