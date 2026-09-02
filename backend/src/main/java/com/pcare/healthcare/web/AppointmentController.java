package com.pcare.healthcare.web;

import com.pcare.healthcare.service.AppointmentService;
import com.pcare.healthcare.web.dto.HealthcareDtos.AppointmentDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.AppointmentStatusRequest;
import com.pcare.healthcare.web.dto.HealthcareDtos.ConsultationRequest;
import com.pcare.healthcare.web.dto.HealthcareDtos.CreateAppointmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Appointments", description = "Doctor appointment workflow, case-linked")
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Operation(summary = "Book an appointment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT','PATIENT')")
    @PostMapping
    public AppointmentDto create(@Valid @RequestBody CreateAppointmentRequest req) {
        return appointmentService.toDto(appointmentService.create(req));
    }

    @Operation(summary = "List appointments for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT','PATIENT')")
    @GetMapping
    public List<AppointmentDto> listByCase(@RequestParam Long caseId) {
        return appointmentService.listByCase(caseId);
    }

    @Operation(summary = "Get an appointment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT','PATIENT')")
    @GetMapping("/{id}")
    public AppointmentDto get(@PathVariable Long id) {
        return appointmentService.toDto(appointmentService.get(id));
    }

    @Operation(summary = "Update appointment status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT')")
    @PutMapping("/{id}/status")
    public AppointmentDto updateStatus(@PathVariable Long id, @Valid @RequestBody AppointmentStatusRequest req) {
        return appointmentService.toDto(appointmentService.updateStatus(id, req.status(), req.note()));
    }

    @Operation(summary = "Record consultation notes and prescription (doctor)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR')")
    @PostMapping("/{id}/consultation")
    public AppointmentDto recordConsultation(@PathVariable Long id, @Valid @RequestBody ConsultationRequest req) {
        return appointmentService.toDto(appointmentService.recordConsultation(id, req));
    }
}
