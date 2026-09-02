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

    public DoctorController(DoctorService doctorService, AppointmentService appointmentService) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
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
}
