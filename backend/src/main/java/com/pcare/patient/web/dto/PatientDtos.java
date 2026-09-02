package com.pcare.patient.web.dto;

import com.pcare.patient.domain.ConsentStatus;
import com.pcare.patient.domain.DocumentType;
import com.pcare.patient.domain.Gender;
import com.pcare.patient.domain.GuardianPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** DTOs for the Patient Management module. */
public final class PatientDtos {

    private PatientDtos() {
    }

    // ---- Patient profile ----
    public record UpsertPatientRequest(
            @NotBlank @Size(max = 160) String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            @Size(max = 30) String mobile,
            @Size(max = 160) String email,
            @Size(max = 400) String addressLine,
            @Size(max = 80) String city,
            @Size(max = 80) String state,
            @Size(max = 80) String country,
            @Size(max = 12) String pincode,
            @Size(max = 120) String emergencyContactName,
            @Size(max = 30) String emergencyContactMobile,
            @Size(max = 60) String emergencyContactRelation,
            @Size(max = 40) String preferredLanguage,
            @Size(max = 40) String identityType,
            @Size(max = 60) String identityNumber,
            @Size(max = 80) String insuranceProvider,
            @Size(max = 60) String insuranceNumber) {
    }

    public record PatientSummaryDto(
            Long id, String fullName, Gender gender, String mobile, String city,
            LocalDate dateOfBirth, Instant createdAt) {
    }

    public record PatientDto(
            Long id, Long userId, String fullName, LocalDate dateOfBirth, Gender gender,
            String mobile, String email, String addressLine, String city, String state, String country,
            String pincode, String emergencyContactName, String emergencyContactMobile,
            String emergencyContactRelation, String preferredLanguage, String identityType,
            String identityNumber, String insuranceProvider, String insuranceNumber,
            Instant createdAt, Instant updatedAt) {
    }

    // ---- Medical profile ----
    public record UpsertMedicalProfileRequest(
            String currentProblem, String diagnosisHistory, String chronicDiseases, String surgeries,
            String previousHospitalisations, String allergies, String currentMedicines,
            @Size(max = 10) String bloodGroup, String disabilityOrMobility, String relevantHistory) {
    }

    public record MedicalProfileDto(
            Long id, Long patientId, String currentProblem, String diagnosisHistory, String chronicDiseases,
            String surgeries, String previousHospitalisations, String allergies, String currentMedicines,
            String bloodGroup, String disabilityOrMobility, String relevantHistory) {
    }

    // ---- Guardian ----
    public record UpsertGuardianRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 60) String relationship,
            @Size(max = 30) String mobile,
            @Size(max = 160) String email,
            Set<GuardianPermission> permissions) {
    }

    public record GuardianDto(
            Long id, Long patientId, String fullName, String relationship, String mobile,
            String email, Set<GuardianPermission> permissions) {
    }

    // ---- Consent ----
    public record UpsertConsentRequest(
            @NotBlank @Size(max = 160) String scope,
            @NotBlank @Size(max = 160) String grantedTo,
            @Size(max = 240) String purpose,
            LocalDate validFrom,
            LocalDate validTo,
            ConsentStatus status) {
    }

    public record ConsentDto(
            Long id, Long patientId, String scope, String grantedTo, String purpose,
            LocalDate validFrom, LocalDate validTo, ConsentStatus status, Instant createdAt) {
    }

    // ---- Document ----
    public record MedicalDocumentDto(
            Long id, Long patientId, DocumentType type, String title, LocalDate documentDate,
            String source, String description, String originalFileName, String contentType,
            Long sizeBytes, boolean shareable, String uploadedBy, Instant createdAt) {
    }

    // ---- Registration wizard (one-shot create) ----
    public record RegistrationWizardRequest(
            @Valid UpsertPatientRequest profile,
            UpsertMedicalProfileRequest medical,
            List<UpsertGuardianRequest> guardians,
            List<UpsertConsentRequest> consents) {
    }
}
