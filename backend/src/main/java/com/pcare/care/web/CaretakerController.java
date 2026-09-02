package com.pcare.care.web;

import com.pcare.care.service.CareService;
import com.pcare.care.web.dto.CareDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Caretakers", description = "Caretaker workforce, assignments and daily activity diary")
@RestController
@RequestMapping("/api/v1")
public class CaretakerController {

    private final CareService careService;

    public CaretakerController(CareService careService) {
        this.careService = careService;
    }

    // ---- Caretaker master ----
    @Operation(summary = "Search / list caretakers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/caretakers")
    public Page<CaretakerDto> search(@RequestParam(required = false) String q,
                                     @PageableDefault(size = 20) Pageable pageable) {
        return careService.searchCaretakers(q, pageable);
    }

    @Operation(summary = "Available caretakers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/caretakers/available")
    public List<CaretakerDto> available() {
        return careService.availableCaretakers();
    }

    @Operation(summary = "Register a caretaker")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/caretakers")
    public CaretakerDto create(@Valid @RequestBody UpsertCaretakerRequest req) {
        return careService.toCaretakerDto(careService.createCaretaker(req));
    }

    @Operation(summary = "Update a caretaker")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/caretakers/{id}")
    public CaretakerDto update(@PathVariable Long id, @Valid @RequestBody UpsertCaretakerRequest req) {
        return careService.toCaretakerDto(careService.updateCaretaker(id, req));
    }

    // ---- Assignments ----
    @Operation(summary = "Assign a caretaker to a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/caretaker-assignments")
    public CaretakerAssignmentDto assign(@Valid @RequestBody AssignCaretakerRequest req) {
        return careService.toAssignmentDto(careService.assign(req));
    }

    @Operation(summary = "List caretaker assignments for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CARETAKER')")
    @GetMapping("/caretaker-assignments")
    public List<CaretakerAssignmentDto> byCase(@RequestParam Long caseId) {
        return careService.assignmentsByCase(caseId);
    }

    @Operation(summary = "Set assignment status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CARETAKER')")
    @PutMapping("/caretaker-assignments/{id}/status")
    public CaretakerAssignmentDto setStatus(@PathVariable Long id, @Valid @RequestBody CareStatusRequest req) {
        return careService.toAssignmentDto(careService.setAssignmentStatus(id, req.status()));
    }

    // ---- Activity diary ----
    @Operation(summary = "Add a daily activity diary entry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CARETAKER')")
    @PostMapping("/caretaker-assignments/{id}/activities")
    public CareActivityDto addActivity(@PathVariable Long id, @Valid @RequestBody AddActivityRequest req) {
        return careService.toActivityDto(careService.addActivity(id, req));
    }

    @Operation(summary = "List activity diary for an assignment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CARETAKER','DOCTOR')")
    @GetMapping("/caretaker-assignments/{id}/activities")
    public List<CareActivityDto> activities(@PathVariable Long id) {
        return careService.activities(id);
    }
}
