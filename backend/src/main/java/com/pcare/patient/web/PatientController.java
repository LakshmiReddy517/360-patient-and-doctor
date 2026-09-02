package com.pcare.patient.web;

import com.pcare.patient.service.PatientService;
import com.pcare.patient.web.dto.PatientDtos.ConsentDto;
import com.pcare.patient.web.dto.PatientDtos.GuardianDto;
import com.pcare.patient.web.dto.PatientDtos.MedicalProfileDto;
import com.pcare.patient.web.dto.PatientDtos.PatientDto;
import com.pcare.patient.web.dto.PatientDtos.PatientSummaryDto;
import com.pcare.patient.web.dto.PatientDtos.RegistrationWizardRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertConsentRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertGuardianRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertMedicalProfileRequest;
import com.pcare.patient.web.dto.PatientDtos.UpsertPatientRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Patients", description = "Patient profile, medical profile, guardians and consent")
@RestController
@RequestMapping("/api/v1/patients")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT','FINANCE')")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Operation(summary = "Search / list patients")
    @GetMapping
    public Page<PatientSummaryDto> search(@RequestParam(required = false) String q,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return patientService.search(q, pageable);
    }

    @Operation(summary = "Create a patient")
    @PostMapping
    public PatientDto create(@Valid @RequestBody UpsertPatientRequest req) {
        return patientService.toDto(patientService.create(req));
    }

    @Operation(summary = "Register a patient via the wizard (profile + medical + guardians + consent)")
    @PostMapping("/register")
    public PatientDto register(@Valid @RequestBody RegistrationWizardRequest req) {
        return patientService.toDto(patientService.register(req));
    }

    @Operation(summary = "Get a patient profile")
    @GetMapping("/{id}")
    public PatientDto get(@PathVariable Long id) {
        return patientService.toDto(patientService.get(id));
    }

    @Operation(summary = "Update a patient profile")
    @PutMapping("/{id}")
    public PatientDto update(@PathVariable Long id, @Valid @RequestBody UpsertPatientRequest req) {
        return patientService.toDto(patientService.update(id, req));
    }

    @Operation(summary = "Archive a patient (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        patientService.archive(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Medical profile ----
    @Operation(summary = "Get patient medical profile")
    @GetMapping("/{id}/medical")
    public MedicalProfileDto getMedical(@PathVariable Long id) {
        return patientService.getMedical(id);
    }

    @Operation(summary = "Create/update patient medical profile")
    @PutMapping("/{id}/medical")
    public MedicalProfileDto upsertMedical(@PathVariable Long id, @Valid @RequestBody UpsertMedicalProfileRequest req) {
        patientService.upsertMedical(id, req);
        return patientService.getMedical(id);
    }

    // ---- Guardians ----
    @Operation(summary = "List guardians")
    @GetMapping("/{id}/guardians")
    public List<GuardianDto> guardians(@PathVariable Long id) {
        return patientService.listGuardians(id);
    }

    @Operation(summary = "Add a guardian")
    @PostMapping("/{id}/guardians")
    public GuardianDto addGuardian(@PathVariable Long id, @Valid @RequestBody UpsertGuardianRequest req) {
        patientService.addGuardian(id, req);
        return patientService.listGuardians(id).get(0);
    }

    @Operation(summary = "Delete a guardian")
    @DeleteMapping("/{id}/guardians/{guardianId}")
    public ResponseEntity<Void> deleteGuardian(@PathVariable Long id, @PathVariable Long guardianId) {
        patientService.deleteGuardian(guardianId);
        return ResponseEntity.noContent().build();
    }

    // ---- Consent ----
    @Operation(summary = "List consent records")
    @GetMapping("/{id}/consents")
    public List<ConsentDto> consents(@PathVariable Long id) {
        return patientService.listConsents(id);
    }

    @Operation(summary = "Add a consent record")
    @PostMapping("/{id}/consents")
    public List<ConsentDto> addConsent(@PathVariable Long id, @Valid @RequestBody UpsertConsentRequest req) {
        patientService.addConsent(id, req);
        return patientService.listConsents(id);
    }

    @Operation(summary = "Revoke a consent record")
    @PostMapping("/{id}/consents/{consentId}/revoke")
    public List<ConsentDto> revokeConsent(@PathVariable Long id, @PathVariable Long consentId) {
        patientService.revokeConsent(consentId);
        return patientService.listConsents(id);
    }
}
