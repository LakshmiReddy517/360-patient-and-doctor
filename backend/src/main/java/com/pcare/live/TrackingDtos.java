package com.pcare.live;

import java.util.List;

/** DTOs for live route tracking (driver -> pickup -> hospital). */
public final class TrackingDtos {

    private TrackingDtos() {
    }

    public record GeoPoint(Double lat, Double lng, String label) {
    }

    public record DriverPoint(Long agentId, String name, Double lat, Double lng, String status) {
    }

    public record RouteDto(
            Long caseId, String caseNumber, String patientName,
            GeoPoint pickup, GeoPoint destination, DriverPoint driver,
            String assignmentStatus,
            /** Road-snapped geometry driver -> pickup -> hospital as [lat,lng] pairs. */
            List<double[]> polyline) {
    }

    public record ActiveJourney(
            Long assignmentId, Long caseId, String caseNumber, String patientName,
            Long agentId, String agentName, String status) {
    }

    public record ActiveJourneyList(List<ActiveJourney> journeys) {
    }
}
