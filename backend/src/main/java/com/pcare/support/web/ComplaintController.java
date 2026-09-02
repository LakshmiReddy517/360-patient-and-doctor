package com.pcare.support.web;

import com.pcare.support.domain.SupportEnums.ComplaintStatus;
import com.pcare.support.service.ComplaintService;
import com.pcare.support.web.dto.SupportDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Complaints", description = "Complaint / grievance lifecycle, comments and SLA")
@RestController
@RequestMapping("/api/v1")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @Operation(summary = "Complaint stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/complaints/stats")
    public ComplaintStats stats() {
        return complaintService.stats();
    }

    @Operation(summary = "Search / list complaints")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/complaints")
    public Page<ComplaintDto> search(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) ComplaintStatus status,
                                     @PageableDefault(size = 20) Pageable pageable) {
        return complaintService.search(q, status, pageable);
    }

    @Operation(summary = "Raise a complaint")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PATIENT')")
    @PostMapping("/complaints")
    public ComplaintDto create(@Valid @RequestBody CreateComplaintRequest req) {
        return complaintService.getDetail(complaintService.create(req).getId());
    }

    @Operation(summary = "Complaint detail with comment history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/complaints/{id}")
    public ComplaintDto detail(@PathVariable Long id) {
        return complaintService.getDetail(id);
    }

    @Operation(summary = "Assign a complaint")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/complaints/{id}/assign")
    public ComplaintDto assign(@PathVariable Long id, @Valid @RequestBody AssignComplaintRequest req) {
        complaintService.assign(id, req);
        return complaintService.getDetail(id);
    }

    @Operation(summary = "Change complaint status (assign/investigate/reopen/close)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/complaints/{id}/status")
    public ComplaintDto setStatus(@PathVariable Long id, @Valid @RequestBody ComplaintStatusRequest req) {
        complaintService.setStatus(id, req.status(), req.note());
        return complaintService.getDetail(id);
    }

    @Operation(summary = "Resolve a complaint")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/complaints/{id}/resolve")
    public ComplaintDto resolve(@PathVariable Long id, @Valid @RequestBody ResolveComplaintRequest req) {
        complaintService.resolve(id, req);
        return complaintService.getDetail(id);
    }

    @Operation(summary = "Add a comment to the complaint")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/complaints/{id}/comments")
    public ComplaintDto addComment(@PathVariable Long id, @Valid @RequestBody AddCommentRequest req) {
        complaintService.addComment(id, req);
        return complaintService.getDetail(id);
    }

    // ---- SLA policy ----
    @Operation(summary = "List SLA policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/sla-policies")
    public List<SlaPolicyDto> slaPolicies() {
        return complaintService.slaPolicies();
    }

    @Operation(summary = "Create/update an SLA policy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/sla-policies")
    public SlaPolicyDto upsertSla(@Valid @RequestBody UpsertSlaPolicyRequest req) {
        return complaintService.upsertSla(req);
    }
}
