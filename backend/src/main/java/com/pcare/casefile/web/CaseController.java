package com.pcare.casefile.web;

import com.pcare.casefile.domain.CaseStatus;
import com.pcare.casefile.service.CaseService;
import com.pcare.casefile.web.dto.CaseDtos.AddNoteRequest;
import com.pcare.casefile.web.dto.CaseDtos.AssignRequest;
import com.pcare.casefile.web.dto.CaseDtos.CaseDetailDto;
import com.pcare.casefile.web.dto.CaseDtos.CaseSummaryDto;
import com.pcare.casefile.web.dto.CaseDtos.CreateCaseRequest;
import com.pcare.casefile.web.dto.CaseDtos.DashboardStats;
import com.pcare.casefile.web.dto.CaseDtos.UpdatePriorityRequest;
import com.pcare.casefile.web.dto.CaseDtos.UpdateStatusRequest;
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

@Tag(name = "Cases", description = "The central Case engine — the 360 view of a patient journey")
@RestController
@RequestMapping("/api/v1/cases")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @Operation(summary = "Command Centre dashboard counts")
    @GetMapping("/dashboard")
    public DashboardStats dashboard() {
        return caseService.dashboard();
    }

    @Operation(summary = "Search / list cases")
    @GetMapping
    public Page<CaseSummaryDto> search(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) CaseStatus status,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return caseService.search(q, status, pageable);
    }

    @Operation(summary = "Create a case")
    @PostMapping
    public CaseSummaryDto create(@Valid @RequestBody CreateCaseRequest req) {
        return caseService.toSummary(caseService.createCase(req));
    }

    @Operation(summary = "Full case detail with timeline")
    @GetMapping("/{id}")
    public CaseDetailDto detail(@PathVariable Long id) {
        return caseService.getDetail(id);
    }

    @Operation(summary = "Change case status")
    @PutMapping("/{id}/status")
    public CaseSummaryDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req) {
        return caseService.toSummary(caseService.updateStatus(id, req));
    }

    @Operation(summary = "Change case priority")
    @PutMapping("/{id}/priority")
    public CaseSummaryDto updatePriority(@PathVariable Long id, @Valid @RequestBody UpdatePriorityRequest req) {
        return caseService.toSummary(caseService.updatePriority(id, req));
    }

    @Operation(summary = "Assign a coordinator to the case")
    @PutMapping("/{id}/assign")
    public CaseSummaryDto assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        return caseService.toSummary(caseService.assign(id, req));
    }

    @Operation(summary = "Add a note to the case timeline")
    @PostMapping("/{id}/notes")
    public CaseSummaryDto addNote(@PathVariable Long id, @Valid @RequestBody AddNoteRequest req) {
        return caseService.toSummary(caseService.addNote(id, req));
    }
}
