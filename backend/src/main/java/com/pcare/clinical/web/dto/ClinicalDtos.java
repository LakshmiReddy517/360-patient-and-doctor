package com.pcare.clinical.web.dto;

import com.pcare.clinical.domain.MedicationIntake.IntakeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

/** DTOs for the clinical module (medicines, intake, patient condition). */
public final class ClinicalDtos {

    private ClinicalDtos() {
    }

    public record UpsertMedicineRequest(
            @NotNull Long caseId, Long patientId, @NotBlank String name, String dose, String route,
            String frequency, LocalDate startDate, LocalDate endDate, String foodInstruction,
            String prescribedBy, String instructions) {
    }

    public record MedicineDto(
            Long id, Long patientId, Long caseId, String name, String dose, String route, String frequency,
            LocalDate startDate, LocalDate endDate, String foodInstruction, String prescribedBy,
            String instructions, boolean active, Instant createdAt) {
    }

    public record RecordIntakeRequest(@NotNull IntakeStatus status, String note) {
    }

    public record IntakeDto(Long id, Long medicineId, Long caseId, String medicineName, IntakeStatus status,
                            String note, String recordedBy, Instant createdAt) {
    }

    public record UpsertConditionRequest(
            @NotNull Long caseId, Long patientId, String condition, String temperature, String bloodPressure,
            String pulse, String spo2, Integer painScore, String notes) {
    }

    public record ConditionDto(
            Long id, Long patientId, Long caseId, String condition, String temperature, String bloodPressure,
            String pulse, String spo2, Integer painScore, String notes, String recordedBy, Instant createdAt) {
    }
}
