package com.pcare.trip;

import com.pcare.trip.TripDtos.Trip;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trip", description = "Unified trip view linking pickup, destination, agent and ambulance (points 44 & 76)")
@RestController
@RequestMapping("/api/v1/cases")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @Operation(summary = "Unified trip for a case (pickup → destination, agent, ambulance, distance, timings)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT','PATIENT')")
    @GetMapping("/{id}/trip")
    public Trip trip(@PathVariable Long id) {
        return tripService.forCase(id);
    }
}
