package com.pcare.clinical.web;

import com.pcare.clinical.service.ClinicalService;
import com.pcare.clinical.web.dto.ClinicalDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clinical", description = "Medicines, medication intake and patient condition/vitals")
@RestController
@RequestMapping("/api/v1/clinical")
public class ClinicalController {

    private final ClinicalService clinicalService;

    public ClinicalController(ClinicalService clinicalService) {
        this.clinicalService = clinicalService;
    }

    // ---- Medicines ----
    @Operation(summary = "Prescribe a medicine (clinician)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR')")
    @PostMapping("/medicines")
    public MedicineDto prescribe(@Valid @RequestBody UpsertMedicineRequest req) {
        return clinicalService.toMedicineDto(clinicalService.prescribe(req));
    }

    @Operation(summary = "List medicines for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','CARETAKER','AGENT')")
    @GetMapping("/medicines")
    public List<MedicineDto> medicines(@RequestParam Long caseId) {
        return clinicalService.medicinesByCase(caseId);
    }

    // ---- Intake ----
    @Operation(summary = "Record a medication intake (taken/missed)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CARETAKER','PATIENT')")
    @PostMapping("/medicines/{id}/intake")
    public IntakeDto recordIntake(@PathVariable Long id, @Valid @RequestBody RecordIntakeRequest req) {
        return clinicalService.toIntakeDto(clinicalService.recordIntake(id, req));
    }

    @Operation(summary = "Medication intake log for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','CARETAKER')")
    @GetMapping("/intake")
    public List<IntakeDto> intake(@RequestParam Long caseId) {
        return clinicalService.intakeByCase(caseId);
    }

    // ---- Patient condition / vitals ----
    @Operation(summary = "Record patient condition / vitals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','CARETAKER')")
    @PostMapping("/conditions")
    public ConditionDto recordCondition(@Valid @RequestBody UpsertConditionRequest req) {
        return clinicalService.toConditionDto(clinicalService.recordCondition(req));
    }

    @Operation(summary = "Patient condition/vitals history for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','CARETAKER')")
    @GetMapping("/conditions")
    public List<ConditionDto> conditions(@RequestParam Long caseId) {
        return clinicalService.conditionsByCase(caseId);
    }
}
