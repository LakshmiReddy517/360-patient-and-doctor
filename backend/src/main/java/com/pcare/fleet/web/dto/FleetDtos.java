package com.pcare.fleet.web.dto;

import com.pcare.fleet.domain.AmbulanceCategory;
import com.pcare.fleet.domain.AmbulanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

/** DTOs for the ambulance fleet module. */
public final class FleetDtos {

    private FleetDtos() {
    }

    // ---- Ambulance ----
    public record UpsertAmbulanceRequest(
            @NotBlank String code, @NotBlank String registrationNo, @NotNull AmbulanceCategory category,
            String ownerName, LocalDate insuranceExpiry, LocalDate fitnessExpiry, LocalDate permitExpiry,
            Long driverId, String driverName) {
    }

    public record AmbulanceDto(
            Long id, String code, String registrationNo, AmbulanceCategory category, String ownerName,
            LocalDate insuranceExpiry, LocalDate fitnessExpiry, LocalDate permitExpiry,
            Long driverId, String driverName, AmbulanceStatus status,
            Double currentLatitude, Double currentLongitude, Instant locationUpdatedAt,
            int oxygenLevelPercent, int fuelPercent, boolean ready, Instant lastCheckAt,
            Long currentCaseId, String currentCaseNumber, String currentPatientName) {
    }

    public record AmbulanceStatusRequest(@NotNull AmbulanceStatus status) {
    }

    public record LocationRequest(@NotNull Double latitude, @NotNull Double longitude) {
    }

    public record AssignAmbulanceRequest(@NotNull Long ambulanceId, @NotNull Long caseId, String note) {
    }

    // ---- Readiness ----
    public record ReadinessRequest(
            boolean fuelOk, boolean engineOk, boolean tyresOk, boolean batteryOk, boolean lightsOk,
            boolean sirenOk, boolean gpsOk, boolean stretcherOk, boolean wheelchairOk, boolean oxygenOk,
            boolean suctionOk, boolean firstAidPpeOk,
            int reportedOxygenPercent, int reportedFuelPercent, String notes) {
    }

    public record ReadinessResult(Long id, Long ambulanceId, boolean passed, String message,
                                  int reportedOxygenPercent, int reportedFuelPercent, Instant createdAt) {
    }

    // ---- EMT / Driver ----
    public record UpsertEmtRequest(
            Long userId, @NotBlank String fullName, String mobile, String licenceNumber,
            LocalDate licenceExpiry, String certification, LocalDate certificationExpiry,
            Boolean medicalFitnessValid, Boolean backgroundVerified, String shift) {
    }

    public record EmtDto(
            Long id, Long userId, String fullName, String mobile, String licenceNumber, LocalDate licenceExpiry,
            String certification, LocalDate certificationExpiry, boolean medicalFitnessValid,
            boolean backgroundVerified, String shift, boolean onDuty) {
    }

    public record FleetStats(long total, long available, long onTrip, long maintenance) {
    }
}
