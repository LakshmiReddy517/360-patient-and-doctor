package com.pcare.healthcare.web.dto;

import com.pcare.healthcare.domain.AppointmentStatus;
import com.pcare.healthcare.domain.BedStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

/** DTOs for the healthcare module. */
public final class HealthcareDtos {

    private HealthcareDtos() {
    }

    // ---- Hospital ----
    public record UpsertHospitalRequest(
            @NotBlank String name, String addressLine, String city, String state, String phone, String email,
            boolean emergencyAvailable, String opdTiming, boolean partner, Boolean active,
            Set<String> departments, Set<String> services) {
    }

    public record HospitalDto(
            Long id, String name, String addressLine, String city, String state, String phone, String email,
            boolean emergencyAvailable, String opdTiming, boolean partner, boolean active,
            Set<String> departments, Set<String> services,
            long totalBeds, long availableBeds) {
    }

    // ---- Bed ----
    public record UpsertBedRequest(
            @NotNull Long hospitalId, String ward, String floor, String roomNumber,
            @NotBlank String bedNumber, String type, BedStatus status) {
    }

    public record BedDto(Long id, Long hospitalId, String ward, String floor, String roomNumber,
                         String bedNumber, String type, BedStatus status) {
    }

    public record BedStatusRequest(@NotNull BedStatus status) {
    }

    // ---- Doctor ----
    public record UpsertDoctorRequest(
            Long userId, @NotBlank String fullName, String specialty, String department,
            Long hospitalId, String hospitalName, String qualification, String registrationNumber,
            String phone, String email, BigDecimal consultationFee, Boolean available) {
    }

    public record DoctorDto(
            Long id, Long userId, String fullName, String specialty, String department,
            Long hospitalId, String hospitalName, String qualification, String registrationNumber,
            String phone, String email, BigDecimal consultationFee, boolean available) {
    }

    // ---- Appointment ----
    public record CreateAppointmentRequest(
            Long caseId, Long patientId, String patientName,
            @NotNull Long doctorId, String department,
            @NotNull LocalDateTime scheduledAt, String reason) {
    }

    public record AppointmentStatusRequest(@NotNull AppointmentStatus status, String note) {
    }

    public record ConsultationRequest(String consultationNotes, String prescription, LocalDateTime followUpAt) {
    }

    public record AppointmentDto(
            Long id, Long caseId, String caseNumber, Long patientId, String patientName,
            Long doctorId, String doctorName, Long hospitalId, String hospitalName, String department,
            LocalDateTime scheduledAt, AppointmentStatus status, String reason,
            String consultationNotes, String prescription, LocalDateTime followUpAt, Instant createdAt) {
    }
}
