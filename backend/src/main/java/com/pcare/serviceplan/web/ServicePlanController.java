package com.pcare.serviceplan.web;

import com.pcare.serviceplan.service.ServicePlanService;
import com.pcare.serviceplan.web.ServicePlanDto.Plan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Service Plan", description = "The aggregated Case Service Plan across all modules")
@RestController
@RequestMapping("/api/v1/cases")
public class ServicePlanController {

    private final ServicePlanService servicePlanService;

    public ServicePlanController(ServicePlanService servicePlanService) {
        this.servicePlanService = servicePlanService;
    }

    @Operation(summary = "Aggregated service plan for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @GetMapping("/{id}/service-plan")
    public Plan servicePlan(@PathVariable Long id) {
        return servicePlanService.build(id);
    }
}
