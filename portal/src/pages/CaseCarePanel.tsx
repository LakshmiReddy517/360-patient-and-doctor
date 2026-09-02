import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { CareActivity, CareActivityType, CareBooking, CareServiceType, Caretaker, CaretakerAssignment } from '../types';

const ACTIVITY_TYPES: CareActivityType[] = ['MEDICINE', 'MEAL', 'VITALS', 'DOCTOR_VISIT', 'REST', 'OBSERVATION', 'HANDOVER'];

export default function CaseCarePanel({ caseId }: { caseId: number }) {
  const [assignments, setAssignments] = useState<CaretakerAssignment[]>([]);
  const [caretakers, setCaretakers] = useState<Caretaker[]>([]);
  const [bookings, setBookings] = useState<CareBooking[]>([]);
  const [pick, setPick] = useState<number | ''>('');
  const [showBooking, setShowBooking] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    Promise.all([
      api.get<CaretakerAssignment[]>('/caretaker-assignments', { params: { caseId } }),
      api.get<{ content: Caretaker[] }>('/caretakers/available').catch(() => ({ data: [] as unknown as { content: Caretaker[] } })),
      api.get<CareBooking[]>('/care-bookings', { params: { caseId } }),
    ]).then(([asg, ct, bk]) => {
      setAssignments(asg.data);
      setCaretakers(Array.isArray(ct.data) ? (ct.data as unknown as Caretaker[]) : []);
      setBookings(bk.data);
    });
  }, [caseId]);
  useEffect(() => { load(); }, [load]);

  async function assign() {
    if (!pick) return;
    setError('');
    try { await api.post('/caretaker-assignments', { caseId, caretakerId: pick, mealsProvided: true, accommodationProvided: false }); setPick(''); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  return (
    <div>
      {error && <div className="error-text">{error}</div>}

      <div className="card card-pad" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3>Caretakers</h3>
          <div style={{ display: 'flex', gap: 8 }}>
            <select className="input" style={{ maxWidth: 220 }} value={pick} onChange={(e) => setPick(Number(e.target.value))}>
              <option value="">Assign a caretaker…</option>
              {caretakers.map((c) => <option key={c.id} value={c.id}>{c.fullName} (₹{c.hourlyRate}/hr)</option>)}
            </select>
            <button className="btn btn-primary btn-sm" onClick={assign} disabled={!pick}>Assign</button>
          </div>
        </div>
        {assignments.length === 0 ? <div className="muted">No caretaker assigned.</div>
          : assignments.map((a) => <CaretakerCard key={a.id} assignment={a} onChanged={load} />)}
      </div>

      <div className="card card-pad">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3>Care bookings</h3>
          <button className="btn btn-primary btn-sm" onClick={() => setShowBooking(true)}>+ New Booking</button>
        </div>
        {bookings.length === 0 ? <div className="muted">No accommodation/food/transport bookings.</div> : (
          <div className="table-wrap">
            <table className="data">
              <thead><tr><th>Type</th><th>Details</th><th>Provider</th><th>Qty × Rate</th><th>Total</th><th>Status</th></tr></thead>
              <tbody>
                {bookings.map((b) => (
                  <tr key={b.id}>
                    <td><span className="badge badge-open">{b.type.replace(/_/g, ' ')}</span></td>
                    <td className="muted" style={{ fontSize: 12 }}>
                      {b.type === 'ACCOMMODATION' && `${b.roomType || ''}`}
                      {b.type === 'FOOD' && `${b.dietType || ''} · ${b.meal || ''}`}
                      {b.type === 'LOCAL_TRANSPORT' && `${b.tripType || ''}: ${b.pickup || ''} → ${b.destination || ''}${b.distanceKm ? ` (${b.distanceKm}km)` : ''}`}
                    </td>
                    <td className="muted">{b.provider || '—'}</td>
                    <td className="muted">{b.quantity} × ₹{b.unitRate}</td>
                    <td className="mono">₹{b.total}</td>
                    <td><span className="badge badge-progress">{b.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showBooking && <BookingModal caseId={caseId} onClose={() => setShowBooking(false)} onDone={() => { setShowBooking(false); load(); }} />}
    </div>
  );
}

function CaretakerCard({ assignment, onChanged }: { assignment: CaretakerAssignment; onChanged: () => void }) {
  const [activities, setActivities] = useState<CareActivity[]>([]);
  const [type, setType] = useState<CareActivityType>('MEDICINE');
  const [note, setNote] = useState('');
  const [open, setOpen] = useState(false);

  const loadActs = useCallback(() => {
    api.get<CareActivity[]>(`/caretaker-assignments/${assignment.id}/activities`).then((r) => setActivities(r.data));
  }, [assignment.id]);
  useEffect(() => { if (open) loadActs(); }, [open, loadActs]);

  async function add() {
    if (!note.trim()) return;
    await api.post(`/caretaker-assignments/${assignment.id}/activities`, { type, note });
    setNote(''); loadActs(); onChanged();
  }

  return (
    <div style={{ padding: '10px 0', borderTop: '1px solid var(--border)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <strong>{assignment.caretakerName}</strong> <span className="muted">· {assignment.shift || 'shift n/a'} · ₹{assignment.hourlyRate}/hr</span>
          <div className="muted" style={{ fontSize: 12 }}>{assignment.responsibilities}</div>
        </div>
        <button className="btn btn-sm" onClick={() => setOpen(!open)}>{open ? 'Hide diary' : 'Diary'}</button>
      </div>
      {open && (
        <div style={{ marginTop: 10 }}>
          <div style={{ display: 'flex', gap: 6, marginBottom: 10 }}>
            <select className="input" style={{ maxWidth: 150 }} value={type} onChange={(e) => setType(e.target.value as CareActivityType)}>
              {ACTIVITY_TYPES.map((t) => <option key={t}>{t}</option>)}
            </select>
            <input className="input" placeholder="Diary note…" value={note} onChange={(e) => setNote(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && add()} />
            <button className="btn btn-primary btn-sm" onClick={add}>Add</button>
          </div>
          <div className="timeline" style={{ marginTop: 8 }}>
            {activities.map((a) => (
              <div className="tl-item" key={a.id}>
                <div className="tl-dot" />
                <div className="tl-title" style={{ fontSize: 13 }}>{a.type}: {a.note}</div>
                <div className="tl-meta">{new Date(a.createdAt).toLocaleString()} · by {a.recordedBy}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function BookingModal({ caseId, onClose, onDone }: { caseId: number; onClose: () => void; onDone: () => void }) {
  const [type, setType] = useState<CareServiceType>('ACCOMMODATION');
  const [provider, setProvider] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [unitRate, setUnitRate] = useState('');
  const [roomType, setRoomType] = useState('');
  const [dietType, setDietType] = useState('');
  const [meal, setMeal] = useState('');
  const [tripType, setTripType] = useState('');
  const [pickup, setPickup] = useState('');
  const [destination, setDestination] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function save() {
    setSaving(true); setError('');
    try {
      await api.post('/care-bookings', {
        caseId, type, provider, quantity: Number(quantity), unitRate: Number(unitRate || 0),
        roomType, dietType, meal, tripType, pickup, destination,
      });
      onDone();
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 }} onClick={onClose}>
      <div className="card card-pad" style={{ width: 460, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 16 }}>New Care Booking</h3>
        {error && <div className="error-text">{error}</div>}
        <div className="field"><label>Type</label>
          <select className="input" value={type} onChange={(e) => setType(e.target.value as CareServiceType)}>
            <option value="ACCOMMODATION">Accommodation</option><option value="FOOD">Food</option><option value="LOCAL_TRANSPORT">Local Transport</option>
          </select>
        </div>
        <div className="field"><label>Provider</label><input className="input" value={provider} onChange={(e) => setProvider(e.target.value)} /></div>
        {type === 'ACCOMMODATION' && <div className="field"><label>Room type</label><input className="input" value={roomType} onChange={(e) => setRoomType(e.target.value)} /></div>}
        {type === 'FOOD' && <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Diet type</label><input className="input" value={dietType} onChange={(e) => setDietType(e.target.value)} /></div>
          <div className="field" style={{ flex: 1 }}><label>Meal</label><input className="input" value={meal} onChange={(e) => setMeal(e.target.value)} /></div>
        </div>}
        {type === 'LOCAL_TRANSPORT' && <>
          <div className="field"><label>Trip type</label><input className="input" value={tripType} onChange={(e) => setTripType(e.target.value)} /></div>
          <div style={{ display: 'flex', gap: 12 }}>
            <div className="field" style={{ flex: 1 }}><label>Pickup</label><input className="input" value={pickup} onChange={(e) => setPickup(e.target.value)} /></div>
            <div className="field" style={{ flex: 1 }}><label>Destination</label><input className="input" value={destination} onChange={(e) => setDestination(e.target.value)} /></div>
          </div>
        </>}
        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field" style={{ flex: 1 }}><label>Quantity</label><input className="input" type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} /></div>
          <div className="field" style={{ flex: 1 }}><label>Unit rate ₹</label><input className="input" type="number" value={unitRate} onChange={(e) => setUnitRate(e.target.value)} /></div>
        </div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Booking…' : 'Book'}</button>
        </div>
      </div>
    </div>
  );
}
