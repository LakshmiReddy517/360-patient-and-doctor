import { useState } from 'react';
import { api, apiErrorMessage } from '../api/client';

/**
 * Service Advisor (blueprint points 55 & 56) — surfaces the backend service-recommendation engine.
 * Turns a coordinator's stated operational needs into suggested OPERATIONAL services. It never
 * diagnoses or gives medical advice; clinical decisions remain with qualified professionals.
 */

interface RecommendRequest {
  travellingAlone: boolean;
  airportOrRailwayArrival: boolean;
  needsAdmission: boolean;
  hasDoctorAppointment: boolean;
  needsAccommodation: boolean;
  mobilityAssistance: boolean;
  extendedStay: boolean;
  notes?: string;
}

interface Recommendation {
  service: string;
  reason: string;
}

interface RecommendResponse {
  recommendations: Recommendation[];
  disclaimer: string;
}

/** Human labels for the seven boolean need flags the engine consumes. */
const NEEDS: { key: keyof Omit<RecommendRequest, 'notes'>; label: string }[] = [
  { key: 'travellingAlone', label: 'Patient travelling alone' },
  { key: 'airportOrRailwayArrival', label: 'Arriving by airport / railway' },
  { key: 'needsAdmission', label: 'Needs hospital admission' },
  { key: 'hasDoctorAppointment', label: 'Has a doctor appointment' },
  { key: 'needsAccommodation', label: 'Needs accommodation' },
  { key: 'mobilityAssistance', label: 'Needs mobility assistance / stretcher' },
  { key: 'extendedStay', label: 'Extended stay' },
];

/** Friendly labels for the service codes the engine can return. */
const SERVICE_LABEL: Record<string, string> = {
  PICKUP_DROP: 'Pickup Drop',
  AMBULANCE: 'Ambulance',
  HOSPITAL_ADMISSION: 'Hospital Admission',
  DOCTOR_APPOINTMENT: 'Doctor Appointment',
  ACCOMMODATION: 'Accommodation',
  CARETAKER: 'Caretaker',
  FOOD: 'Food',
  LOCAL_TRANSPORT: 'Local Transport',
};

function serviceLabel(s: string): string {
  return SERVICE_LABEL[s] ?? s.replace(/_/g, ' ');
}

export default function ServiceAdvisorPage() {
  const [form, setForm] = useState<RecommendRequest>({
    travellingAlone: false,
    airportOrRailwayArrival: false,
    needsAdmission: false,
    hasDoctorAppointment: false,
    needsAccommodation: false,
    mobilityAssistance: false,
    extendedStay: false,
    notes: '',
  });
  const [result, setResult] = useState<RecommendResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  function toggle(key: keyof Omit<RecommendRequest, 'notes'>, value: boolean) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function getRecommendations() {
    setError('');
    setLoading(true);
    try {
      const r = await api.post<RecommendResponse>('/recommendations', form);
      setResult(r.data);
    } catch (e) {
      setError(apiErrorMessage(e));
      setResult(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div className="page-head">
        <h2>Service Advisor</h2>
        <span className="muted" style={{ fontSize: 13 }}>
          Suggests OPERATIONAL services from the patient's stated needs. This is not medical advice —
          it does not diagnose or prescribe; clinical decisions remain with qualified professionals.
        </span>
      </div>

      {error && <div className="error-text">{error}</div>}

      <div className="grid-2">
        <div className="card card-pad">
          <h3 style={{ marginBottom: 4 }}>Stated needs</h3>
          <div className="muted" style={{ fontSize: 12, marginBottom: 14 }}>
            Tick everything that applies, then generate a recommended set of services.
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {NEEDS.map((n) => (
              <label key={n.key} style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 13.5 }}>
                <input
                  type="checkbox"
                  checked={form[n.key]}
                  onChange={(e) => toggle(n.key, e.target.checked)}
                />
                {n.label}
              </label>
            ))}
          </div>

          <div className="field" style={{ marginTop: 14 }}>
            <label>Notes (optional)</label>
            <textarea
              className="input"
              rows={3}
              placeholder="Any additional context for the coordinator…"
              value={form.notes ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
            />
          </div>

          <button
            className="btn btn-primary"
            style={{ marginTop: 14 }}
            onClick={getRecommendations}
            disabled={loading}
          >
            {loading ? 'Getting recommendations…' : 'Get recommendations'}
          </button>
        </div>

        <div className="card card-pad">
          <h3 style={{ marginBottom: 4 }}>Recommended services</h3>
          <div className="muted" style={{ fontSize: 12, marginBottom: 14 }}>
            Operational suggestions generated by the recommendation engine.
          </div>

          {loading && <div className="spinner">Getting recommendations…</div>}

          {!loading && !result && (
            <div className="muted" style={{ fontSize: 13 }}>
              Fill in the stated needs and select "Get recommendations".
            </div>
          )}

          {!loading && result && (
            <>
              {result.recommendations.length === 0 ? (
                <div className="muted" style={{ fontSize: 13 }}>No services recommended for the stated needs.</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {result.recommendations.map((rec) => (
                    <div
                      key={rec.service}
                      style={{ display: 'flex', gap: 10, alignItems: 'baseline', padding: '10px 0', borderBottom: '1px solid var(--border)' }}
                    >
                      <span className="badge badge-open" style={{ whiteSpace: 'nowrap' }}>
                        {serviceLabel(rec.service)}
                      </span>
                      <span className="muted" style={{ fontSize: 13 }}>{rec.reason}</span>
                    </div>
                  ))}
                </div>
              )}

              {result.disclaimer && (
                <div style={{ background: 'var(--warning-soft)', color: 'var(--warning)', padding: '8px 12px', borderRadius: 8, fontSize: 11.5, marginTop: 14 }}>
                  ⚕ {result.disclaimer}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
