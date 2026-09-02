package com.pcare.healthcare.web;

import com.pcare.healthcare.service.HospitalService;
import com.pcare.healthcare.web.dto.HealthcareDtos.BedDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.BedStatusRequest;
import com.pcare.healthcare.web.dto.HealthcareDtos.HospitalDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertBedRequest;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertHospitalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Hospitals", description = "Hospital master and bed/room management")
@RestController
@RequestMapping("/api/v1")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @Operation(summary = "Search / list hospitals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT','PATIENT')")
    @GetMapping("/hospitals")
    public Page<HospitalDto> search(@RequestParam(required = false) String q,
                                    @PageableDefault(size = 20) Pageable pageable) {
        return hospitalService.search(q, pageable);
    }

    @Operation(summary = "Get a hospital")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT','PATIENT')")
    @GetMapping("/hospitals/{id}")
    public HospitalDto get(@PathVariable Long id) {
        return hospitalService.getDto(id);
    }

    @Operation(summary = "Create a hospital")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/hospitals")
    public HospitalDto create(@Valid @RequestBody UpsertHospitalRequest req) {
        return hospitalService.toDto(hospitalService.create(req));
    }

    @Operation(summary = "Update a hospital")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/hospitals/{id}")
    public HospitalDto update(@PathVariable Long id, @Valid @RequestBody UpsertHospitalRequest req) {
        return hospitalService.toDto(hospitalService.update(id, req));
    }

    // ---- Beds ----
    @Operation(summary = "List beds for a hospital")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT')")
    @GetMapping("/hospitals/{id}/beds")
    public List<BedDto> beds(@PathVariable Long id) {
        return hospitalService.listBeds(id);
    }

    @Operation(summary = "Add a bed")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/beds")
    public BedDto addBed(@Valid @RequestBody UpsertBedRequest req) {
        return hospitalService.toBedDto(hospitalService.addBed(req));
    }

    @Operation(summary = "Set bed status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PutMapping("/beds/{id}/status")
    public void setBedStatus(@PathVariable Long id, @Valid @RequestBody BedStatusRequest req) {
        hospitalService.setBedStatus(id, req.status());
    }
}
