package com.pcare.ops;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Command-Centre operational dashboard — one aggregated view across cases, agents and fleet. */
@Tag(name = "Operations", description = "Aggregated command-centre operational metrics")
@RestController
@RequestMapping("/api/v1/ops")
public class OperationsController {

    private final OperationsService operationsService;

    public OperationsController(OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @Operation(summary = "Unified operational dashboard (requests, cases, agents, fleet, dispatch)")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    public OperationsService.OpsDashboard dashboard() {
        return operationsService.dashboard();
    }
}
