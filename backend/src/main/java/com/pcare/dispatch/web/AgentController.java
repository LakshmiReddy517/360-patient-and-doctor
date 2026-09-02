package com.pcare.dispatch.web;

import com.pcare.dispatch.service.AgentService;
import com.pcare.dispatch.service.AssignmentService;
import com.pcare.dispatch.web.dto.DispatchDtos.AgentDto;
import com.pcare.dispatch.web.dto.DispatchDtos.AssignmentDto;
import com.pcare.dispatch.web.dto.DispatchDtos.DispatchStats;
import com.pcare.dispatch.web.dto.DispatchDtos.LocationRequest;
import com.pcare.dispatch.web.dto.DispatchDtos.StatusRequest;
import com.pcare.dispatch.web.dto.DispatchDtos.UpsertAgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Agents", description = "Care-agent workforce, availability and live location")
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentService agentService;
    private final AssignmentService assignmentService;
    private final com.pcare.live.TrackingService trackingService;

    public AgentController(AgentService agentService, AssignmentService assignmentService,
                          com.pcare.live.TrackingService trackingService) {
        this.agentService = agentService;
        this.assignmentService = assignmentService;
        this.trackingService = trackingService;
    }

    @Operation(summary = "Dispatch stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/stats")
    public DispatchStats stats() {
        return agentService.stats();
    }

    @Operation(summary = "List agents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping
    public List<AgentDto> list() {
        return agentService.listAll();
    }

    @Operation(summary = "The agent profile linked to the logged-in account (agent app)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @GetMapping("/me")
    public AgentDto me() {
        Long uid = com.pcare.security.SecurityUtils.currentUserId()
                .orElseThrow(() -> new com.pcare.common.exception.NotFoundException("Not authenticated"));
        return agentService.toDto(agentService.getByUserId(uid));
    }

    @Operation(summary = "List dispatchable (online/available) agents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/available")
    public List<AgentDto> available() {
        return agentService.listDispatchable();
    }

    @Operation(summary = "Register an agent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping
    public AgentDto create(@Valid @RequestBody UpsertAgentRequest req) {
        return agentService.toDto(agentService.create(req));
    }

    @Operation(summary = "Get an agent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @GetMapping("/{id}")
    public AgentDto get(@PathVariable Long id) {
        return agentService.toDto(agentService.get(id));
    }

    @Operation(summary = "Update an agent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}")
    public AgentDto update(@PathVariable Long id, @Valid @RequestBody UpsertAgentRequest req) {
        return agentService.toDto(agentService.update(id, req));
    }

    @Operation(summary = "Set agent availability status (agent app)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PutMapping("/{id}/status")
    public AgentDto setStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return agentService.toDto(agentService.setStatus(id, req.status()));
    }

    @Operation(summary = "Post live GPS location (agent app)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PutMapping("/{id}/location")
    public AgentDto updateLocation(@PathVariable Long id, @Valid @RequestBody LocationRequest req) {
        return agentService.toDto(agentService.updateLocation(id, req.latitude(), req.longitude()));
    }

    @Operation(summary = "List an agent's assignments (agent app job feed)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @GetMapping("/{id}/assignments")
    public List<AssignmentDto> assignments(@PathVariable Long id) {
        return assignmentService.listByAgent(id);
    }

    @Operation(summary = "Live route (self -> pickup -> hospital) for one of the agent's cases (agent app map)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @GetMapping("/{agentId}/cases/{caseId}/route")
    public com.pcare.live.TrackingDtos.RouteDto caseRoute(@PathVariable Long agentId, @PathVariable Long caseId) {
        boolean assigned = assignmentService.listByAgent(agentId).stream()
                .anyMatch(a -> caseId.equals(a.caseId()));
        if (!assigned) {
            throw new com.pcare.common.exception.NotFoundException("No assignment for this case");
        }
        return trackingService.route(caseId);
    }
}
