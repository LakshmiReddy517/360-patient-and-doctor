package com.pcare.fleet.web;

import com.pcare.fleet.service.FleetService;
import com.pcare.fleet.web.dto.FleetDtos.AmbulanceDto;
import com.pcare.fleet.web.dto.FleetDtos.AmbulanceStatusRequest;
import com.pcare.fleet.web.dto.FleetDtos.AssignAmbulanceRequest;
import com.pcare.fleet.web.dto.FleetDtos.FleetStats;
import com.pcare.fleet.web.dto.FleetDtos.LocationRequest;
import com.pcare.fleet.web.dto.FleetDtos.ReadinessRequest;
import com.pcare.fleet.web.dto.FleetDtos.ReadinessResult;
import com.pcare.fleet.web.dto.FleetDtos.UpsertAmbulanceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Ambulances", description = "Fleet management, readiness, dispatch and live tracking")
@RestController
@RequestMapping("/api/v1")
public class AmbulanceController {

    private final FleetService fleetService;

    public AmbulanceController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @Operation(summary = "Fleet stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/ambulances/stats")
    public FleetStats stats() {
        return fleetService.stats();
    }

    @Operation(summary = "List ambulances")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EMT')")
    @GetMapping("/ambulances")
    public List<AmbulanceDto> list() {
        return fleetService.listAll();
    }

    @Operation(summary = "Live tracking — active (non-offline) ambulances")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/ambulances/tracking")
    public List<AmbulanceDto> tracking() {
        return fleetService.listActive();
    }

    @Operation(summary = "Get an ambulance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EMT')")
    @GetMapping("/ambulances/{id}")
    public AmbulanceDto get(@PathVariable Long id) {
        return fleetService.toDto(fleetService.get(id));
    }

    @Operation(summary = "Register an ambulance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/ambulances")
    public AmbulanceDto create(@Valid @RequestBody UpsertAmbulanceRequest req) {
        return fleetService.toDto(fleetService.create(req));
    }

    @Operation(summary = "Update an ambulance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/ambulances/{id}")
    public AmbulanceDto update(@PathVariable Long id, @Valid @RequestBody UpsertAmbulanceRequest req) {
        return fleetService.toDto(fleetService.update(id, req));
    }

    @Operation(summary = "Set ambulance status (EMT app / control room)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EMT')")
    @PutMapping("/ambulances/{id}/status")
    public AmbulanceDto setStatus(@PathVariable Long id, @Valid @RequestBody AmbulanceStatusRequest req) {
        return fleetService.toDto(fleetService.setStatus(id, req.status()));
    }

    @Operation(summary = "Post ambulance GPS location (EMT app)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EMT')")
    @PutMapping("/ambulances/{id}/location")
    public AmbulanceDto location(@PathVariable Long id, @Valid @RequestBody LocationRequest req) {
        return fleetService.toDto(fleetService.updateLocation(id, req.latitude(), req.longitude()));
    }

    @Operation(summary = "Submit pre-duty readiness checklist (EMT app)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EMT')")
    @PostMapping("/ambulances/{id}/readiness")
    public ReadinessResult readiness(@PathVariable Long id, @Valid @RequestBody ReadinessRequest req) {
        return fleetService.submitReadiness(id, req);
    }

    @Operation(summary = "Dispatch an ambulance to a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/ambulances/assign")
    public AmbulanceDto assign(@Valid @RequestBody AssignAmbulanceRequest req) {
        return fleetService.toDto(fleetService.assign(req.ambulanceId(), req.caseId(), req.note()));
    }
}
