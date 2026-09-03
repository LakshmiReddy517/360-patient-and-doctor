package com.pcare.healthcare.web;

import com.pcare.healthcare.service.AppointmentService;
import com.pcare.healthcare.service.DoctorService;
import com.pcare.healthcare.web.dto.HealthcareDtos.AppointmentDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.DoctorDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertDoctorRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Doctors", description = "Doctor registry and appointments")
@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final com.pcare.clinical.service.ClinicalService clinicalService;

    public DoctorController(DoctorService doctorService, AppointmentService appointmentService,
                            com.pcare.clinical.service.ClinicalService clinicalService) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.clinicalService = clinicalService;
    }

    @Operation(summary = "Search / list doctors")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT','PATIENT')")
    @GetMapping
    public Page<DoctorDto> search(@RequestParam(required = false) String q,
                                  @PageableDefault(size = 20) Pageable pageable) {
        return doctorService.search(q, pageable);
    }

    @Operation(summary = "Get a doctor")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT','PATIENT')")
    @GetMapping("/{id}")
    public DoctorDto get(@PathVariable Long id) {
        return doctorService.toDto(doctorService.get(id));
    }

    @Operation(summary = "Register a doctor")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping
    public DoctorDto create(@Valid @RequestBody UpsertDoctorRequest req) {
        return doctorService.toDto(doctorService.create(req));
    }

    @Operation(summary = "Update a doctor")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}")
    public DoctorDto update(@PathVariable Long id, @Valid @RequestBody UpsertDoctorRequest req) {
        return doctorService.toDto(doctorService.update(id, req));
    }

    @Operation(summary = "A doctor's appointments (doctor app/portal)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR')")
    @GetMapping("/{id}/appointments")
    public List<AppointmentDto> appointments(@PathVariable Long id) {
        return appointmentService.listByDoctor(id);
    }

    @Operation(summary = "Doctor dashboard: appointment counts, upcoming and pending reviews (doctor app)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR')")
    @GetMapping("/{id}/dashboard")
    public java.util.Map<String, Object> dashboard(@PathVariable Long id) {
        List<AppointmentDto> appts = appointmentService.listByDoctor(id);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long upcoming = appts.stream().filter(a -> a.scheduledAt() != null && a.scheduledAt().isAfter(now)
                && a.status() != com.pcare.healthcare.domain.AppointmentStatus.CANCELLED
                && a.status() != com.pcare.healthcare.domain.AppointmentStatus.COMPLETED).count();
        long completed = appts.stream().filter(a -> a.status() == com.pcare.healthcare.domain.AppointmentStatus.COMPLETED).count();
        long pendingReview = appts.stream().filter(a -> a.status() == com.pcare.healthcare.domain.AppointmentStatus.ARRIVED
                || a.status() == com.pcare.healthcare.domain.AppointmentStatus.IN_CONSULTATION).count();
        long followUps = appts.stream().filter(a -> a.followUpAt() != null).count();
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("doctorId", id);
        m.put("totalAppointments", appts.size());
        m.put("upcoming", upcoming);
        m.put("completed", completed);
        m.put("pendingReview", pendingReview);
        m.put("followUps", followUps);
        m.put("next", appts.stream()
                .filter(a -> a.scheduledAt() != null && a.scheduledAt().isAfter(now))
                .sorted(java.util.Comparator.comparing(AppointmentDto::scheduledAt)).limit(5).toList());
        return m;
    }

    @Operation(summary = "Consolidated clinical view of a case for the treating doctor (appointments, vitals, medicines, intake)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR')")
    @GetMapping("/cases/{caseId}/clinical")
    public java.util.Map<String, Object> caseClinical(@PathVariable Long caseId) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("caseId", caseId);
        m.put("appointments", appointmentService.listByCase(caseId));
        m.put("vitals", clinicalService.conditionsByCase(caseId));
        m.put("medicines", clinicalService.medicinesByCase(caseId));
        m.put("medicationIntake", clinicalService.intakeByCase(caseId));
        return m;
    }
}
