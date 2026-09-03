package com.pcare.accommodation;

import com.pcare.accommodation.AccommodationDtos.AccommodationDto;
import com.pcare.accommodation.AccommodationDtos.NearbyAccommodationDto;
import com.pcare.accommodation.AccommodationDtos.UpsertAccommodationRequest;
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

@Tag(name = "Accommodation", description = "Accommodation master near treating hospitals (blueprint point 42)")
@RestController
@RequestMapping("/api/v1/accommodations")
public class AccommodationController {

    private final AccommodationService accommodationService;

    public AccommodationController(AccommodationService accommodationService) {
        this.accommodationService = accommodationService;
    }

    @Operation(summary = "List active accommodation options, optionally filtered by city (cheapest first)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','AGENT','PATIENT')")
    @GetMapping
    public List<AccommodationDto> list(@RequestParam(required = false) String city) {
        return accommodationService.list(city);
    }

    @Operation(summary = "Accommodation options within a radius of a lat/lng, sorted by distance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','AGENT','PATIENT')")
    @GetMapping("/nearby")
    public List<NearbyAccommodationDto> nearby(@RequestParam double latitude, @RequestParam double longitude,
                                               @RequestParam(defaultValue = "10") double radiusKm) {
        return accommodationService.nearby(latitude, longitude, radiusKm);
    }

    @Operation(summary = "Get a single accommodation option")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','AGENT','PATIENT')")
    @GetMapping("/{id}")
    public AccommodationDto get(@PathVariable Long id) {
        return accommodationService.get(id);
    }

    @Operation(summary = "Add an accommodation option to the master (admin)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping
    public AccommodationDto create(@Valid @RequestBody UpsertAccommodationRequest req) {
        return accommodationService.create(req);
    }

    @Operation(summary = "Update an accommodation option (admin)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}")
    public AccommodationDto update(@PathVariable Long id, @Valid @RequestBody UpsertAccommodationRequest req) {
        return accommodationService.update(id, req);
    }
}
