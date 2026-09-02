package com.pcare.nearby;

import com.pcare.nearby.NearbyDtos.NearbyResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GPS-based discovery of nearby hospitals & ambulances for the patient app. */
@Tag(name = "Nearby", description = "Find nearby hospitals and ambulances from a device location")
@RestController
@RequestMapping("/api/v1/nearby")
public class NearbyController {

    private final NearbyService nearbyService;

    public NearbyController(NearbyService nearbyService) {
        this.nearbyService = nearbyService;
    }

    @Operation(summary = "Nearest hospitals and ambulances to a lat/lng (any authenticated user)")
    @GetMapping
    public NearbyResult nearby(@RequestParam double lat, @RequestParam double lng,
                               @RequestParam(defaultValue = "8") int limit) {
        return nearbyService.nearby(lat, lng, limit);
    }
}
