package com.pcare.trip;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A unified Trip view (blueprint points 44 & 76): one object linking a case's pickup and destination
 * legs, the assigned agent, the ambulance, distance and journey timings — assembled read-only from
 * the service-request, dispatch and fleet modules.
 */
public final class TripDtos {

    private TripDtos() {
    }

    public record TripLeg(String label, String address, Double latitude, Double longitude) {
    }

    public record Trip(
            Long caseId,
            String caseNumber,
            String patientName,
            String phase,              // NOT_STARTED / EN_ROUTE_PICKUP / PATIENT_ONBOARD / AT_DESTINATION / COMPLETED
            // agent
            Long agentId,
            String agentName,
            String assignmentStatus,
            String pickupOtp,
            String handoverOtp,
            // ambulance
            Long ambulanceId,
            String ambulanceRegistration,
            String ambulanceCategory,
            String ambulanceStatus,
            // legs
            TripLeg pickup,
            TripLeg destination,
            Double distanceKm,
            LocalDateTime appointmentAt,
            // timings
            Instant acceptedAt,
            Instant pickedAt,
            Instant completedAt) {
    }
}
