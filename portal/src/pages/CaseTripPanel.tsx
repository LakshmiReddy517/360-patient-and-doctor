import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Trip } from '../types';

const PHASE_BADGE: Record<string, string> = {
  NOT_STARTED: 'badge-normal', EN_ROUTE_PICKUP: 'badge-hold', PATIENT_ONBOARD: 'badge-progress',
  AT_DESTINATION: 'badge-open', COMPLETED: 'badge-closed',
};

const PHASES = ['NOT_STARTED', 'EN_ROUTE_PICKUP', 'PATIENT_ONBOARD', 'AT_DESTINATION', 'COMPLETED'];

export default function CaseTripPanel({ caseId }: { caseId: number }) {
  const [trip, setTrip] = useState<Trip | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<Trip>(`/cases/${caseId}/trip`).then((r) => setTrip(r.data)).finally(() => setLoading(false));
  }, [caseId]);

  if (loading) return <div className="spinner">Loading trip…</div>;
  if (!trip) return null;

  const activeIdx = PHASES.indexOf(trip.phase);

  return (
    <div>
      <div className="card card-pad" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3>Trip — {trip.caseNumber}</h3>
          <span className={'badge ' + (PHASE_BADGE[trip.phase] || 'badge-normal')}>{trip.phase.replace(/_/g, ' ')}</span>
        </div>
        {/* Journey phase strip */}
        <div style={{ display: 'flex', gap: 6, marginTop: 14, flexWrap: 'wrap' }}>
          {PHASES.map((p, i) => (
            <span key={p} className={'badge ' + (i <= activeIdx && activeIdx >= 0 ? 'badge-progress' : 'badge-normal')}
              style={{ opacity: i <= activeIdx && activeIdx >= 0 ? 1 : 0.5 }}>
              {i + 1}. {p.replace(/_/g, ' ')}
            </span>
          ))}
        </div>
      </div>

      <div className="grid-2">
        <div className="card card-pad">
          <h3 style={{ marginBottom: 12 }}>Route</h3>
          <div className="kv"><span className="k">Pickup</span><span>{trip.pickup?.address || '—'}</span></div>
          <div className="kv"><span className="k">Pickup coords</span><span className="mono">{trip.pickup?.latitude != null ? `${trip.pickup.latitude}, ${trip.pickup.longitude}` : '—'}</span></div>
          <div className="kv"><span className="k">Destination</span><span>{trip.destination?.label || '—'}</span></div>
          <div className="kv"><span className="k">Dest. address</span><span>{trip.destination?.address || '—'}</span></div>
          <div className="kv"><span className="k">Dest. coords</span><span className="mono">{trip.destination?.latitude != null ? `${trip.destination.latitude}, ${trip.destination.longitude}` : '—'}</span></div>
          <div className="kv"><span className="k">Distance</span><span className="mono" style={{ fontWeight: 700, color: 'var(--primary)' }}>{trip.distanceKm != null ? trip.distanceKm + ' km' : '—'}</span></div>
          {trip.appointmentAt && <div className="kv"><span className="k">Appointment</span><span>{new Date(trip.appointmentAt).toLocaleString()}</span></div>}
        </div>

        <div>
          <div className="card card-pad">
            <h3 style={{ marginBottom: 12 }}>Agent & Vehicle</h3>
            <div className="kv"><span className="k">Agent</span><span>{trip.agentName || 'Unassigned'}</span></div>
            <div className="kv"><span className="k">Assignment</span><span>{trip.assignmentStatus?.replace(/_/g, ' ') || '—'}</span></div>
            <div className="kv"><span className="k">Pickup OTP</span><span className="mono">{trip.pickupOtp || '—'}</span></div>
            <div className="kv"><span className="k">Handover OTP</span><span className="mono">{trip.handoverOtp || '—'}</span></div>
            <div className="kv"><span className="k">Ambulance</span><span>{trip.ambulanceRegistration ? `${trip.ambulanceRegistration} (${trip.ambulanceCategory})` : '—'}</span></div>
            <div className="kv"><span className="k">Vehicle status</span><span>{trip.ambulanceStatus?.replace(/_/g, ' ') || '—'}</span></div>
          </div>
          <div className="card card-pad" style={{ marginTop: 20 }}>
            <h3 style={{ marginBottom: 12 }}>Timings</h3>
            <div className="kv"><span className="k">Accepted</span><span>{trip.acceptedAt ? new Date(trip.acceptedAt).toLocaleString() : '—'}</span></div>
            <div className="kv"><span className="k">Patient picked</span><span>{trip.pickedAt ? new Date(trip.pickedAt).toLocaleString() : '—'}</span></div>
            <div className="kv"><span className="k">Completed</span><span>{trip.completedAt ? new Date(trip.completedAt).toLocaleString() : '—'}</span></div>
          </div>
        </div>
      </div>
    </div>
  );
}
