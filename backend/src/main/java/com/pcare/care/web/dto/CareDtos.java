package com.pcare.care.web.dto;

import com.pcare.care.domain.CareEnums.CareActivityType;
import com.pcare.care.domain.CareEnums.CareServiceType;
import com.pcare.care.domain.CareEnums.CareStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

/** DTOs for the care-services module. */
public final class CareDtos {

    private CareDtos() {
    }

    // ---- Caretaker (master) ----
    public record UpsertCaretakerRequest(
            Long userId, @NotBlank String fullName, String mobile, Set<String> skills,
            Set<String> languages, String shift, BigDecimal hourlyRate, Boolean verified, Boolean available) {
    }

    public record CaretakerDto(
            Long id, Long userId, String fullName, String mobile, Set<String> skills, Set<String> languages,
            String shift, BigDecimal hourlyRate, boolean verified, boolean available) {
    }

    // ---- Caretaker assignment ----
    public record AssignCaretakerRequest(
            @NotNull Long caseId, @NotNull Long caretakerId, LocalDateTime startAt, LocalDateTime endAt,
            String shift, BigDecimal hourlyRate, String responsibilities,
            boolean mealsProvided, boolean accommodationProvided) {
    }

    public record CaretakerAssignmentDto(
            Long id, Long caseId, String caseNumber, String patientName, Long caretakerId, String caretakerName,
            LocalDateTime startAt, LocalDateTime endAt, String shift, BigDecimal hourlyRate, String responsibilities,
            boolean mealsProvided, boolean accommodationProvided, CareStatus status, Instant createdAt) {
    }

    public record CareStatusRequest(@NotNull CareStatus status) {
    }

    // ---- Care activity diary ----
    public record AddActivityRequest(@NotNull CareActivityType type, @NotBlank String note) {
    }

    public record CareActivityDto(Long id, Long assignmentId, Long caseId, CareActivityType type,
                                  String note, String recordedBy, Instant createdAt) {
    }

    // ---- Unified care booking (accommodation / food / transport) ----
    public record CreateCareBookingRequest(
            @NotNull Long caseId, @NotNull CareServiceType type, String provider, String description,
            LocalDateTime startAt, LocalDateTime endAt, BigDecimal quantity, BigDecimal unitRate,
            String roomType, String dietType, String meal, String tripType, String pickup,
            String destination, Double distanceKm) {
    }

    public record CareBookingDto(
            Long id, Long caseId, String caseNumber, String patientName, CareServiceType type, String provider,
            String description, LocalDateTime startAt, LocalDateTime endAt, BigDecimal quantity, BigDecimal unitRate,
            BigDecimal total, String roomType, String dietType, String meal, String tripType, String pickup,
            String destination, Double distanceKm, CareStatus status, Instant createdAt) {
    }
}
