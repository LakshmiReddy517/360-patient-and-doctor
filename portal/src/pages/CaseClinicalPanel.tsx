import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorMessage } from '../api/client';
import type { Medicine, MedicationIntake, PatientConditionReading } from '../types';

export default function CaseClinicalPanel({ caseId }: { caseId: number }) {
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [intake, setIntake] = useState<MedicationIntake[]>([]);
  const [vitals, setVitals] = useState<PatientConditionReading[]>([]);
  const [showRx, setShowRx] = useState(false);
  const [showVitals, setShowVitals] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    Promise.all([
      api.get<Medicine[]>('/clinical/medicines', { params: { caseId } }),
      api.get<MedicationIntake[]>('/clinical/intake', { params: { caseId } }),
      api.get<PatientConditionReading[]>('/clinical/conditions', { params: { caseId } }),
    ]).then(([m, i, v]) => { setMedicines(m.data); setIntake(i.data); setVitals(v.data); });
  }, [caseId]);
  useEffect(() => { load(); }, [load]);

  async function recordIntake(medId: number, status: 'TAKEN' | 'MISSED') {
    setError('');
    try { await api.post(`/clinical/medicines/${medId}/intake`, { status }); load(); }
    catch (e) { setError(apiErrorMessage(e)); }
  }

  return (
    <div>
      {error && <div className="error-text">{error}</div>}
      <div style={{ background: 'var(--warning-soft)', color: 'var(--warning)', padding: '8px 12px', borderRadius: 8, fontSize: 12.5, marginBottom: 16 }}>
        ⚕ Recorded by the authorised clinician. The platform stores and reminds — it does not diagnose, prescribe or change medication.
      </div>

      <div className="card card-pad" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3>Medicines</h3>
          <button className="btn btn-primary btn-sm" onClick={() => setShowRx(true)}>+ Prescribe</button>
        </div>
        {medicines.length === 0 ? <div className="muted">No medicines prescribed.</div> : (
          <div className="table-wrap">
            <table className="data">
              <thead><tr><th>Medicine</th><th>Dose</th><th>Frequency</th><th>Food</th><th>By</th><th>Intake</th></tr></thead>
              <tbody>
                {medicines.map((m) => (
                  <tr key={m.id}>
                    <td style={{ fontWeight: 600 }}>{m.name}<div className="muted" style={{ fontWeight: 400, fontSize: 11 }}>{m.route} · {m.instructions}</div></td>
                    <td>{m.dose || '—'}</td>
                    <td className="mono">{m.frequency || '—'}</td>
                    <td className="muted">{m.foodInstruction || '—'}</td>
                    <td className="muted">{m.prescribedBy || '—'}</td>
                    <td><div style={{ display: 'flex', gap: 4 }}><button className="btn btn-sm" onClick={() => recordIntake(m.id, 'TAKEN')}>Taken</button><button className="btn btn-sm" onClick={() => recordIntake(m.id, 'MISSED')}>Missed</button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {intake.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <h4 style={{ marginBottom: 6, fontSize: 13 }}>Adherence log</h4>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {intake.map((i) => (
                <span key={i.id} className={'badge ' + (i.status === 'TAKEN' ? 'badge-progress' : 'badge-emergency')} title={new Date(i.createdAt).toLocaleString()}>
                  {i.status === 'TAKEN' ? '✓' : '✗'} {i.medicineName}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="card card-pad">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3>Patient condition / vitals</h3>
          <button className="btn btn-primary btn-sm" onClick={() => setShowVitals(true)}>+ Record vitals</button>
        </div>
        {vitals.length === 0 ? <div className="muted">No vitals recorded.</div> : (
          <div className="table-wrap">
            <table className="data">
              <thead><tr><th>When</th><th>Condition</th><th>Temp</th><th>BP</th><th>Pulse</th><th>SpO₂</th><th>Pain</th><th>By</th></tr></thead>
              <tbody>
                {vitals.map((v) => (
                  <tr key={v.id}>
                    <td className="muted">{new Date(v.createdAt).toLocaleString()}</td>
                    <td>{v.condition || '—'}</td>
                    <td className="mono">{v.temperature || '—'}</td>
                    <td className="mono">{v.bloodPressure || '—'}</td>
                    <td className="mono">{v.pulse || '—'}</td>
                    <td className="mono" style={{ color: v.spo2 && parseInt(v.spo2) < 92 ? 'var(--danger)' : undefined }}>{v.spo2 || '—'}</td>
                    <td>{v.painScore ?? '—'}</td>
                    <td className="muted">{v.recordedBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showRx && <PrescribeModal caseId={caseId} onClose={() => setShowRx(false)} onDone={() => { setShowRx(false); load(); }} />}
      {showVitals && <VitalsModal caseId={caseId} onClose={() => setShowVitals(false)} onDone={() => { setShowVitals(false); load(); }} />}
    </div>
  );
}

const overlay: React.CSSProperties = { position: 'fixed', inset: 0, background: 'rgba(15,23,42,.5)', display: 'grid', placeItems: 'center', zIndex: 50 };

function PrescribeModal({ caseId, onClose, onDone }: { caseId: number; onClose: () => void; onDone: () => void }) {
  const [f, setF] = useState({ name: '', dose: '', route: 'Oral', frequency: '1-0-1', foodInstruction: 'After food', prescribedBy: '', instructions: '' });
  const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  const set = (k: string, v: string) => setF((o) => ({ ...o, [k]: v }));
  async function save() {
    setSaving(true); setError('');
    try { await api.post('/clinical/medicines', { caseId, ...f }); onDone(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }
  return (
    <div style={overlay} onClick={onClose}><div className="card card-pad" style={{ width: 460, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
      <h3 style={{ marginBottom: 16 }}>Prescribe Medicine</h3>
      {error && <div className="error-text">{error}</div>}
      <div className="field"><label>Name *</label><input className="input" value={f.name} onChange={(e) => set('name', e.target.value)} /></div>
      <div style={{ display: 'flex', gap: 12 }}>
        <div className="field" style={{ flex: 1 }}><label>Dose</label><input className="input" value={f.dose} onChange={(e) => set('dose', e.target.value)} /></div>
        <div className="field" style={{ flex: 1 }}><label>Route</label><input className="input" value={f.route} onChange={(e) => set('route', e.target.value)} /></div>
        <div className="field" style={{ flex: 1 }}><label>Frequency</label><input className="input" value={f.frequency} onChange={(e) => set('frequency', e.target.value)} /></div>
      </div>
      <div style={{ display: 'flex', gap: 12 }}>
        <div className="field" style={{ flex: 1 }}><label>Food</label><input className="input" value={f.foodInstruction} onChange={(e) => set('foodInstruction', e.target.value)} /></div>
        <div className="field" style={{ flex: 1 }}><label>Prescribed by</label><input className="input" value={f.prescribedBy} onChange={(e) => set('prescribedBy', e.target.value)} /></div>
      </div>
      <div className="field"><label>Instructions</label><input className="input" value={f.instructions} onChange={(e) => set('instructions', e.target.value)} /></div>
      <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}><button className="btn" onClick={onClose}>Cancel</button><button className="btn btn-primary" onClick={save} disabled={saving || !f.name}>{saving ? 'Saving…' : 'Prescribe'}</button></div>
    </div></div>
  );
}

function VitalsModal({ caseId, onClose, onDone }: { caseId: number; onClose: () => void; onDone: () => void }) {
  const [f, setF] = useState({ condition: '', temperature: '', bloodPressure: '', pulse: '', spo2: '', painScore: '', notes: '' });
  const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  const set = (k: string, v: string) => setF((o) => ({ ...o, [k]: v }));
  async function save() {
    setSaving(true); setError('');
    try { await api.post('/clinical/conditions', { caseId, ...f, painScore: f.painScore ? Number(f.painScore) : null }); onDone(); }
    catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }
  return (
    <div style={overlay} onClick={onClose}><div className="card card-pad" style={{ width: 460, maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}>
      <h3 style={{ marginBottom: 16 }}>Record Vitals</h3>
      {error && <div className="error-text">{error}</div>}
      <div className="field"><label>Condition</label><input className="input" value={f.condition} onChange={(e) => set('condition', e.target.value)} /></div>
      <div style={{ display: 'flex', gap: 12 }}>
        <div className="field" style={{ flex: 1 }}><label>Temp</label><input className="input" value={f.temperature} onChange={(e) => set('temperature', e.target.value)} /></div>
        <div className="field" style={{ flex: 1 }}><label>BP</label><input className="input" value={f.bloodPressure} onChange={(e) => set('bloodPressure', e.target.value)} /></div>
      </div>
      <div style={{ display: 'flex', gap: 12 }}>
        <div className="field" style={{ flex: 1 }}><label>Pulse</label><input className="input" value={f.pulse} onChange={(e) => set('pulse', e.target.value)} /></div>
        <div className="field" style={{ flex: 1 }}><label>SpO₂</label><input className="input" value={f.spo2} onChange={(e) => set('spo2', e.target.value)} /></div>
        <div className="field" style={{ flex: 1 }}><label>Pain (0-10)</label><input className="input" type="number" value={f.painScore} onChange={(e) => set('painScore', e.target.value)} /></div>
      </div>
      <div className="field"><label>Notes</label><input className="input" value={f.notes} onChange={(e) => set('notes', e.target.value)} /></div>
      <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}><button className="btn" onClick={onClose}>Cancel</button><button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Record'}</button></div>
    </div></div>
  );
}
