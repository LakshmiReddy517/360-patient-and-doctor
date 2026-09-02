package com.pcare.live;

import com.pcare.common.exception.NotFoundException;
import com.pcare.live.TrackingDtos.ActiveJourneyList;
import com.pcare.live.TrackingDtos.RouteDto;
import com.pcare.security.JwtService;
import com.pcare.servicerequest.domain.ServiceRequest;
import com.pcare.servicerequest.repo.ServiceRequestRepository;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Live journey tracking for the Command Centre map and the patient "track my ambulance" view. */
@Tag(name = "Live", description = "Live route tracking")
@RestController
public class TrackingController {

    private final TrackingService trackingService;
    private final JwtService jwtService;
    private final ServiceRequestRepository requestRepository;

    public TrackingController(TrackingService trackingService, JwtService jwtService,
                              ServiceRequestRepository requestRepository) {
        this.trackingService = trackingService;
        this.jwtService = jwtService;
        this.requestRepository = requestRepository;
    }

    // ---- Command Centre (bearer auth, admin) ----

    @Operation(summary = "List active journeys (assignments in flight)")
    @GetMapping("/api/v1/live/active-journeys")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    public ActiveJourneyList activeJourneys() {
        return new ActiveJourneyList(trackingService.activeJourneys());
    }

    @Operation(summary = "Route points (pickup, destination, live driver) for a case")
    @GetMapping("/api/v1/live/route/{caseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    public RouteDto route(@PathVariable Long caseId) {
        return trackingService.route(caseId);
    }

    // ---- Patient "track my ambulance" (token query param, used by the in-app WebView) ----

    @Operation(summary = "Route for one of my cases (token in query param, like the SSE stream)")
    @GetMapping("/api/v1/track/route")
    public RouteDto myRoute(@RequestParam Long caseId, @RequestParam String token) {
        Claims claims = jwtService.parse(token);   // throws if invalid/expired
        String username = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        boolean admin = roles != null && (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN"));

        ServiceRequest sr = requestRepository.findFirstByCaseId(caseId)
                .orElseThrow(() -> new NotFoundException("No journey found for case " + caseId));
        if (!admin && !username.equals(sr.getCreatedBy())) {
            throw new NotFoundException("No journey found for this account");
        }
        return trackingService.route(caseId);
    }
}
