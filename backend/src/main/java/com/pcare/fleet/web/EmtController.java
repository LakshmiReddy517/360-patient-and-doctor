package com.pcare.fleet.web;

import com.pcare.fleet.service.EmtService;
import com.pcare.fleet.web.dto.FleetDtos.EmtDto;
import com.pcare.fleet.web.dto.FleetDtos.UpsertEmtRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "EMT / Drivers", description = "Ambulance driver / EMT registry")
@RestController
@RequestMapping("/api/v1/emts")
public class EmtController {

    private final EmtService emtService;

    public EmtController(EmtService emtService) {
        this.emtService = emtService;
    }

    @Operation(summary = "List drivers/EMTs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping
    public List<EmtDto> list() {
        return emtService.listAll();
    }

    @Operation(summary = "Register a driver/EMT")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping
    public EmtDto create(@Valid @RequestBody UpsertEmtRequest req) {
        return emtService.toDto(emtService.create(req));
    }

    @Operation(summary = "Update a driver/EMT")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}")
    public EmtDto update(@PathVariable Long id, @Valid @RequestBody UpsertEmtRequest req) {
        return emtService.toDto(emtService.update(id, req));
    }

    @Operation(summary = "Set duty on/off (EMT app)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EMT')")
    @PutMapping("/{id}/duty")
    public EmtDto setDuty(@PathVariable Long id, @RequestParam boolean onDuty) {
        return emtService.toDto(emtService.setDuty(id, onDuty));
    }
}
