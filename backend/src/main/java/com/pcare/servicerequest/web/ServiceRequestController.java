package com.pcare.servicerequest.web;

import com.pcare.servicerequest.domain.RequestStatus;
import com.pcare.servicerequest.service.ServiceRequestService;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.CreateRequest;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.RequestDetailDto;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.RequestStats;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.RequestSummaryDto;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.TransitionRequest;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.TriageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Service Requests", description = "Booking flow: request, triage, lifecycle and conversion to a Case")
@RestController
@RequestMapping("/api/v1/requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @Operation(summary = "Book a service request (patient or admin on behalf)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PATIENT')")
    @PostMapping
    public RequestDetailDto create(@Valid @RequestBody CreateRequest req) {
        return service.toDetail(service.create(req));
    }

    @Operation(summary = "Queue stats for the Command Centre")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/stats")
    public RequestStats stats() {
        return service.stats();
    }

    @Operation(summary = "Search / list requests")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping
    public Page<RequestSummaryDto> search(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) RequestStatus status,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return service.search(q, status, pageable);
    }

    @Operation(summary = "Request detail")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @GetMapping("/{id}")
    public RequestDetailDto detail(@PathVariable Long id) {
        return service.toDetail(service.get(id));
    }

    @Operation(summary = "Triage a request (emergency assessment)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}/triage")
    public RequestDetailDto triage(@PathVariable Long id, @Valid @RequestBody TriageRequest req) {
        return service.toDetail(service.triage(id, req));
    }

    @Operation(summary = "Transition request status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}/status")
    public RequestDetailDto transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest req) {
        return service.toDetail(service.transition(id, req));
    }

    @Operation(summary = "Convert request into a Case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/{id}/convert-to-case")
    public RequestDetailDto convert(@PathVariable Long id) {
        return service.toDetail(service.convertToCase(id));
    }
}
