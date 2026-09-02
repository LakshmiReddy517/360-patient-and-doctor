package com.pcare.dispatch.web;

import com.pcare.dispatch.service.AssignmentService;
import com.pcare.dispatch.web.dto.DispatchDtos.AssignRequest;
import com.pcare.dispatch.web.dto.DispatchDtos.AssignmentDto;
import com.pcare.dispatch.web.dto.DispatchDtos.AssignmentStatusRequest;
import com.pcare.dispatch.web.dto.DispatchDtos.VerifyOtpRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dispatch", description = "Assign agents to cases and drive the pickup/handover workflow")
@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @Operation(summary = "Assign an agent to a case (dispatch)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping
    public AssignmentDto assign(@Valid @RequestBody AssignRequest req) {
        return assignmentService.toDto(assignmentService.assign(req.agentId(), req.caseId(), req.notes()));
    }

    @Operation(summary = "List assignments for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @GetMapping
    public List<AssignmentDto> listByCase(@RequestParam Long caseId) {
        return assignmentService.listByCase(caseId);
    }

    @Operation(summary = "Agent accepts an offered assignment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PostMapping("/{id}/accept")
    public AssignmentDto accept(@PathVariable Long id) {
        return assignmentService.toDto(assignmentService.accept(id));
    }

    @Operation(summary = "Update assignment workflow status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PutMapping("/{id}/status")
    public AssignmentDto updateStatus(@PathVariable Long id, @Valid @RequestBody AssignmentStatusRequest req) {
        return assignmentService.toDto(assignmentService.updateStatus(id, req.status(), req.note()));
    }

    @Operation(summary = "Verify pickup OTP")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PostMapping("/{id}/verify-pickup")
    public AssignmentDto verifyPickup(@PathVariable Long id, @Valid @RequestBody VerifyOtpRequest req) {
        return assignmentService.toDto(assignmentService.verifyPickup(id, req.otp()));
    }

    @Operation(summary = "Verify handover OTP (completes the assignment)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PostMapping("/{id}/verify-handover")
    public AssignmentDto verifyHandover(@PathVariable Long id, @Valid @RequestBody VerifyOtpRequest req) {
        return assignmentService.toDto(assignmentService.verifyHandover(id, req.otp()));
    }

    @Operation(summary = "Cancel an assignment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/{id}/cancel")
    public AssignmentDto cancel(@PathVariable Long id, @RequestBody(required = false) AssignmentStatusRequest req) {
        return assignmentService.toDto(assignmentService.cancel(id, req != null ? req.note() : null));
    }
}
