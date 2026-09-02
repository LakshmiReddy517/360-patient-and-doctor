import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, apiErrorMessage } from '../api/client';
import type { Gender, GuardianPermission } from '../types';

const STEPS = ['Personal', 'Contact', 'Medical', 'Guardian & Consent', 'Review'];
const ALL_PERMS: GuardianPermission[] = ['VIEW_LOCATION', 'VIEW_RECORDS', 'RECEIVE_NOTIFICATIONS', 'APPROVE_SERVICES', 'MAKE_PAYMENTS'];

export default function PatientWizardPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  // Personal
  const [fullName, setFullName] = useState('');
  const [dateOfBirth, setDob] = useState('');
  const [gender, setGender] = useState<Gender>('MALE');
  const [preferredLanguage, setLang] = useState('');
  // Contact
  const [mobile, setMobile] = useState('');
  const [email, setEmail] = useState('');
  const [addressLine, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [country, setCountry] = useState('India');
  const [ecName, setEcName] = useState('');
  const [ecMobile, setEcMobile] = useState('');
  const [ecRelation, setEcRelation] = useState('');
  // Medical
  const [currentProblem, setCurrentProblem] = useState('');
  const [chronicDiseases, setChronic] = useState('');
  const [allergies, setAllergies] = useState('');
  const [currentMedicines, setMeds] = useState('');
  const [bloodGroup, setBlood] = useState('');
  // Guardian
  const [gName, setGName] = useState('');
  const [gRelation, setGRelation] = useState('');
  const [gMobile, setGMobile] = useState('');
  const [gPerms, setGPerms] = useState<GuardianPermission[]>([]);
  // Consent
  const [consentScope, setConsentScope] = useState('Medical records');
  const [consentTo, setConsentTo] = useState('Assigned care agent');
  const [consentGiven, setConsentGiven] = useState(false);

  function togglePerm(p: GuardianPermission) {
    setGPerms((cur) => (cur.includes(p) ? cur.filter((x) => x !== p) : [...cur, p]));
  }

  async function submit() {
    setError(''); setSaving(true);
    try {
      const body = {
        profile: {
          fullName, dateOfBirth: dateOfBirth || null, gender, preferredLanguage,
          mobile, email, addressLine, city, state, country,
          emergencyContactName: ecName, emergencyContactMobile: ecMobile, emergencyContactRelation: ecRelation,
        },
        medical: { currentProblem, chronicDiseases, allergies, currentMedicines, bloodGroup },
        guardians: gName ? [{ fullName: gName, relationship: gRelation, mobile: gMobile, permissions: gPerms }] : [],
        consents: consentGiven ? [{ scope: consentScope, grantedTo: consentTo, purpose: 'Service coordination' }] : [],
      };
      const { data } = await api.post('/patients/register', body);
      navigate(`/patients/${data.id}`);
    } catch (e) { setError(apiErrorMessage(e)); } finally { setSaving(false); }
  }

  const canNext = step === 0 ? fullName.trim().length > 0 : true;

  return (
    <div>
      <div className="page-head">
        <div>
          <span className="link" onClick={() => navigate('/patients')}>← Patients</span>
          <h2 style={{ marginTop: 6 }}>Register Patient</h2>
        </div>
      </div>

      {/* Stepper */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 20, flexWrap: 'wrap' }}>
        {STEPS.map((s, i) => (
          <div key={s} style={{
            display: 'flex', alignItems: 'center', gap: 8, padding: '6px 12px', borderRadius: 999,
            background: i === step ? 'var(--primary)' : i < step ? 'var(--primary-soft)' : 'var(--surface-2)',
            color: i === step ? '#fff' : i < step ? 'var(--primary-dark)' : 'var(--text-muted)',
            fontSize: 13, fontWeight: 600, border: '1px solid var(--border)',
          }}>
            <span>{i < step ? '✓' : i + 1}</span> {s}
          </div>
        ))}
      </div>

      {error && <div className="error-text">{error}</div>}

      <div className="card card-pad" style={{ maxWidth: 720 }}>
        {step === 0 && (
          <>
            <div className="field"><label>Full name *</label><input className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} autoFocus /></div>
            <div style={{ display: 'flex', gap: 16 }}>
              <div className="field" style={{ flex: 1 }}><label>Date of birth</label><input className="input" type="date" value={dateOfBirth} onChange={(e) => setDob(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>Gender</label>
                <select className="input" value={gender} onChange={(e) => setGender(e.target.value as Gender)}>
                  <option>MALE</option><option>FEMALE</option><option>OTHER</option><option>UNDISCLOSED</option>
                </select>
              </div>
            </div>
            <div className="field"><label>Preferred language</label><input className="input" value={preferredLanguage} onChange={(e) => setLang(e.target.value)} /></div>
          </>
        )}

        {step === 1 && (
          <>
            <div style={{ display: 'flex', gap: 16 }}>
              <div className="field" style={{ flex: 1 }}><label>Mobile</label><input className="input" value={mobile} onChange={(e) => setMobile(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>Email</label><input className="input" value={email} onChange={(e) => setEmail(e.target.value)} /></div>
            </div>
            <div className="field"><label>Address</label><input className="input" value={addressLine} onChange={(e) => setAddress(e.target.value)} /></div>
            <div style={{ display: 'flex', gap: 16 }}>
              <div className="field" style={{ flex: 1 }}><label>City</label><input className="input" value={city} onChange={(e) => setCity(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>State</label><input className="input" value={state} onChange={(e) => setState(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>Country</label><input className="input" value={country} onChange={(e) => setCountry(e.target.value)} /></div>
            </div>
            <h4 style={{ margin: '10px 0' }}>Emergency contact</h4>
            <div style={{ display: 'flex', gap: 16 }}>
              <div className="field" style={{ flex: 1 }}><label>Name</label><input className="input" value={ecName} onChange={(e) => setEcName(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>Mobile</label><input className="input" value={ecMobile} onChange={(e) => setEcMobile(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>Relation</label><input className="input" value={ecRelation} onChange={(e) => setEcRelation(e.target.value)} /></div>
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <div style={{ background: 'var(--warning-soft)', color: 'var(--warning)', padding: '8px 12px', borderRadius: 8, fontSize: 12.5, marginBottom: 16 }}>
              ⚕ Recorded as provided by the patient. Clinical decisions remain with qualified clinicians.
            </div>
            <div className="field"><label>Current problem</label><textarea className="input" rows={2} value={currentProblem} onChange={(e) => setCurrentProblem(e.target.value)} /></div>
            <div style={{ display: 'flex', gap: 16 }}>
              <div className="field" style={{ flex: 1 }}><label>Chronic diseases</label><input className="input" value={chronicDiseases} onChange={(e) => setChronic(e.target.value)} /></div>
              <div className="field" style={{ width: 120 }}><label>Blood group</label><input className="input" value={bloodGroup} onChange={(e) => setBlood(e.target.value)} /></div>
            </div>
            <div className="field"><label>Allergies</label><input className="input" value={allergies} onChange={(e) => setAllergies(e.target.value)} /></div>
            <div className="field"><label>Current medicines</label><input className="input" value={currentMedicines} onChange={(e) => setMeds(e.target.value)} /></div>
          </>
        )}

        {step === 3 && (
          <>
            <h4 style={{ marginBottom: 10 }}>Guardian (optional)</h4>
            <div style={{ display: 'flex', gap: 16 }}>
              <div className="field" style={{ flex: 1 }}><label>Name</label><input className="input" value={gName} onChange={(e) => setGName(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>Relationship</label><input className="input" value={gRelation} onChange={(e) => setGRelation(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>Mobile</label><input className="input" value={gMobile} onChange={(e) => setGMobile(e.target.value)} /></div>
            </div>
            <label style={{ fontSize: 12.5, fontWeight: 600 }}>Permissions granted to guardian</label>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '8px 0 20px' }}>
              {ALL_PERMS.map((p) => (
                <label key={p} style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 12.5, padding: '5px 10px', border: '1px solid var(--border)', borderRadius: 8, cursor: 'pointer' }}>
                  <input type="checkbox" checked={gPerms.includes(p)} onChange={() => togglePerm(p)} /> {p.replace(/_/g, ' ')}
                </label>
              ))}
            </div>
            <h4 style={{ marginBottom: 10 }}>Consent</h4>
            <div style={{ display: 'flex', gap: 16 }}>
              <div className="field" style={{ flex: 1 }}><label>Share</label><input className="input" value={consentScope} onChange={(e) => setConsentScope(e.target.value)} /></div>
              <div className="field" style={{ flex: 1 }}><label>With</label><input className="input" value={consentTo} onChange={(e) => setConsentTo(e.target.value)} /></div>
            </div>
            <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 13 }}>
              <input type="checkbox" checked={consentGiven} onChange={(e) => setConsentGiven(e.target.checked)} />
              Patient consents to the above information being shared for service coordination.
            </label>
          </>
        )}

        {step === 4 && (
          <div>
            <h3 style={{ marginBottom: 12 }}>Review</h3>
            <div className="kv"><span className="k">Name</span><span>{fullName || '—'}</span></div>
            <div className="kv"><span className="k">Gender / DOB</span><span>{gender} · {dateOfBirth || '—'}</span></div>
            <div className="kv"><span className="k">Mobile / City</span><span>{mobile || '—'} · {city || '—'}</span></div>
            <div className="kv"><span className="k">Blood group</span><span>{bloodGroup || '—'}</span></div>
            <div className="kv"><span className="k">Chronic / Allergies</span><span>{chronicDiseases || '—'} / {allergies || '—'}</span></div>
            <div className="kv"><span className="k">Guardian</span><span>{gName ? `${gName} (${gPerms.length} permissions)` : 'None'}</span></div>
            <div className="kv"><span className="k">Consent</span><span>{consentGiven ? `${consentScope} → ${consentTo}` : 'Not given'}</span></div>
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 24 }}>
          <button className="btn" disabled={step === 0} onClick={() => setStep((s) => s - 1)}>← Back</button>
          {step < STEPS.length - 1 ? (
            <button className="btn btn-primary" disabled={!canNext} onClick={() => setStep((s) => s + 1)}>Next →</button>
          ) : (
            <button className="btn btn-primary" disabled={saving || !fullName} onClick={submit}>{saving ? 'Registering…' : 'Register Patient'}</button>
          )}
        </div>
      </div>
    </div>
  );
}
