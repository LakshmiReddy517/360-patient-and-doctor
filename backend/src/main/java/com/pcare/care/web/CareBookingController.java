package com.pcare.care.web;

import com.pcare.care.service.CareService;
import com.pcare.care.web.dto.CareDtos.CareBookingDto;
import com.pcare.care.web.dto.CareDtos.CareStatusRequest;
import com.pcare.care.web.dto.CareDtos.CreateCareBookingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Care Bookings", description = "Accommodation, food and local transport bookings (case-linked)")
@RestController
@RequestMapping("/api/v1/care-bookings")
public class CareBookingController {

    private final CareService careService;

    public CareBookingController(CareService careService) {
        this.careService = careService;
    }

    @Operation(summary = "Create a care booking (accommodation / food / local transport)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PostMapping
    public CareBookingDto create(@Valid @RequestBody CreateCareBookingRequest req) {
        return careService.toBookingDto(careService.createBooking(req));
    }

    @Operation(summary = "List care bookings for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT','PATIENT')")
    @GetMapping
    public List<CareBookingDto> byCase(@RequestParam Long caseId) {
        return careService.bookingsByCase(caseId);
    }

    @Operation(summary = "Set care booking status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PutMapping("/{id}/status")
    public CareBookingDto setStatus(@PathVariable Long id, @Valid @RequestBody CareStatusRequest req) {
        return careService.toBookingDto(careService.setBookingStatus(id, req.status()));
    }
}
