package com.pcare.servicerequest.web.dto;

import com.pcare.servicerequest.domain.PickupSource;
import com.pcare.servicerequest.domain.RequestStatus;
import com.pcare.servicerequest.domain.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

/** DTOs for the Service Request module. */
public final class ServiceRequestDtos {

    private ServiceRequestDtos() {
    }

    public record PickupDto(
            PickupSource source, String address, Double latitude, Double longitude,
            LocalDateTime scheduledAt, String flightOrTrainNumber, LocalDateTime arrivalTime,
            String terminalOrCoach, String seatOrBerth) {
    }

    public record DestinationDto(
            String destinationType, String hospitalName, String department, String doctorName,
            String address, LocalDateTime appointmentAt) {
    }

    public record CreateRequest(
            Long patientId,
            @NotBlank @Size(max = 160) String patientName,
            @Size(max = 30) String patientMobile,
            @NotEmpty Set<ServiceType> services,
            @Size(max = 1000) String notes,
            boolean emergency,
            PickupDto pickup,
            DestinationDto destination) {
    }

    public record TriageRequest(
            boolean emergency,
            @Size(max = 500) String triageNotes) {
    }

    public record TransitionRequest(
            @NotNull RequestStatus status,
            @Size(max = 500) String note) {
    }

    public record RequestSummaryDto(
            Long id, Long patientId, String patientName, String patientMobile,
            Set<ServiceType> services, RequestStatus status, boolean emergency,
            Long caseId, String caseNumber, Instant createdAt) {
    }

    public record RequestDetailDto(
            Long id, Long patientId, String patientName, String patientMobile,
            Set<ServiceType> services, String notes, RequestStatus status, boolean emergency,
            String triageNotes, Long caseId, String caseNumber,
            PickupDto pickup, DestinationDto destination, Instant createdAt, Instant updatedAt) {
    }

    public record RequestStats(
            long newRequests, long underReview, long triage, long confirmed, long inProgress) {
    }
}
