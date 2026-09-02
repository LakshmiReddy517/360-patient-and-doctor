package com.pcare.support.web;

import com.pcare.support.domain.SupportEnums.IncidentStatus;
import com.pcare.support.service.IncidentService;
import com.pcare.support.web.dto.SupportDtos.CreateIncidentRequest;
import com.pcare.support.web.dto.SupportDtos.IncidentDto;
import com.pcare.support.web.dto.SupportDtos.IncidentStatusRequest;
import com.pcare.support.web.dto.SupportDtos.IncidentStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Incidents", description = "Operational incident management and escalation")
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @Operation(summary = "Incident stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/stats")
    public IncidentStats stats() {
        return incidentService.stats();
    }

    @Operation(summary = "Search / list incidents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT','EMT')")
    @GetMapping
    public Page<IncidentDto> search(@RequestParam(required = false) IncidentStatus status,
                                    @PageableDefault(size = 20) Pageable pageable) {
        return incidentService.search(status, pageable);
    }

    @Operation(summary = "Report an incident")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT','EMT')")
    @PostMapping
    public IncidentDto create(@Valid @RequestBody CreateIncidentRequest req) {
        return incidentService.toDto(incidentService.create(req));
    }

    @Operation(summary = "Update incident status / escalate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}/status")
    public IncidentDto updateStatus(@PathVariable Long id, @Valid @RequestBody IncidentStatusRequest req) {
        return incidentService.toDto(incidentService.updateStatus(id, req.status(), req.resolution()));
    }
}
