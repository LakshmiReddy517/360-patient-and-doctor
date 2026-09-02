package com.pcare.live;

import com.pcare.common.exception.NotFoundException;
import com.pcare.dispatch.domain.Agent;
import com.pcare.dispatch.domain.Assignment;
import com.pcare.dispatch.domain.AssignmentStatus;
import com.pcare.dispatch.repo.AgentRepository;
import com.pcare.dispatch.repo.AssignmentRepository;
import com.pcare.live.TrackingDtos.ActiveJourney;
import com.pcare.live.TrackingDtos.DriverPoint;
import com.pcare.live.TrackingDtos.GeoPoint;
import com.pcare.live.TrackingDtos.RouteDto;
import com.pcare.servicerequest.domain.DestinationInfo;
import com.pcare.servicerequest.domain.PickupInfo;
import com.pcare.servicerequest.domain.ServiceRequest;
import com.pcare.servicerequest.repo.ServiceRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the three points of an ambulance journey — live driver, patient pickup and hospital
 * destination — so the Command Centre can draw the route on a map.
 */
@Service
public class TrackingService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrackingService.class);

    private final ServiceRequestRepository requestRepository;
    private final AssignmentRepository assignmentRepository;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    /** Per-case cache of the last computed road route, so we don't call OSRM on every 2s poll. */
    private record CachedRoute(double lat, double lng, List<double[]> polyline) {}
    private final java.util.Map<Long, CachedRoute> routeCache = new ConcurrentHashMap<>();

    public TrackingService(ServiceRequestRepository requestRepository, AssignmentRepository assignmentRepository,
                           AgentRepository agentRepository) {
        this.requestRepository = requestRepository;
        this.assignmentRepository = assignmentRepository;
        this.agentRepository = agentRepository;
    }

    /** Journeys currently in flight (assignments not yet completed/cancelled), newest first. */
    @Transactional(readOnly = true)
    public List<ActiveJourney> activeJourneys() {
        return assignmentRepository
                .findByStatusNotInOrderByCreatedAtDesc(List.of(AssignmentStatus.COMPLETED, AssignmentStatus.CANCELLED))
                .stream()
                .map(a -> new ActiveJourney(a.getId(), a.getCaseId(), a.getCaseNumber(), a.getPatientName(),
                        a.getAgentId(), a.getAgentName(), a.getStatus().name()))
                .toList();
    }

    /** The pickup, destination and live driver position for a case's active assignment. */
    @Transactional(readOnly = true)
    public RouteDto route(Long caseId) {
        ServiceRequest sr = requestRepository.findFirstByCaseId(caseId)
                .orElseThrow(() -> new NotFoundException("No service request linked to case " + caseId));

        GeoPoint pickup = pickupPoint(sr.getPickup());
        GeoPoint destination = destinationPoint(sr.getDestination());

        // Latest non-closed assignment for the case gives us the driver.
        Assignment assignment = assignmentRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream()
                .filter(a -> !a.getStatus().isClosed())
                .findFirst()
                .orElse(null);

        DriverPoint driver = null;
        String assignmentStatus = null;
        if (assignment != null) {
            assignmentStatus = assignment.getStatus().name();
            Agent agent = agentRepository.findById(assignment.getAgentId()).orElse(null);
            if (agent != null) {
                driver = new DriverPoint(agent.getId(), agent.getFullName(),
                        agent.getCurrentLatitude(), agent.getCurrentLongitude(),
                        agent.getStatus().name());
            }
        }

        List<double[]> polyline = buildPolyline(caseId, driver, pickup, destination);

        return new RouteDto(caseId, sr.getCaseNumber(), sr.getPatientName(),
                pickup, destination, driver, assignmentStatus, polyline);
    }

    /** Road-snapped geometry driver -> pickup -> hospital, cached until the driver moves ~150m. */
    private List<double[]> buildPolyline(Long caseId, DriverPoint driver, GeoPoint pickup, GeoPoint destination) {
        List<double[]> waypoints = new ArrayList<>();
        if (driver != null && driver.lat() != null && driver.lng() != null) waypoints.add(new double[]{driver.lat(), driver.lng()});
        if (pickup != null) waypoints.add(new double[]{pickup.lat(), pickup.lng()});
        if (destination != null) waypoints.add(new double[]{destination.lat(), destination.lng()});
        if (waypoints.size() < 2) return waypoints;

        double dLat = waypoints.get(0)[0], dLng = waypoints.get(0)[1];
        CachedRoute cached = routeCache.get(caseId);
        if (cached != null && haversineMeters(cached.lat(), cached.lng(), dLat, dLng) < 150) {
            return cached.polyline();
        }
        List<double[]> road = osrmRoad(waypoints);
        routeCache.put(caseId, new CachedRoute(dLat, dLng, road));
        return road;
    }

    private List<double[]> osrmRoad(List<double[]> waypoints) {
        try {
            StringBuilder coords = new StringBuilder();
            for (double[] p : waypoints) {
                if (coords.length() > 0) coords.append(';');
                coords.append(p[1]).append(',').append(p[0]);
            }
            String url = "https://router.project-osrm.org/route/v1/driving/" + coords
                    + "?overview=full&geometries=geojson";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(6)).header("User-Agent", "pcare-backend").GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode coordsNode = objectMapper.readTree(resp.body())
                    .path("routes").get(0).path("geometry").path("coordinates");
            List<double[]> out = new ArrayList<>();
            for (JsonNode c : coordsNode) out.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
            return out.isEmpty() ? waypoints : out;
        } catch (Exception e) {
            log.warn("OSRM road fetch failed: {}", e.getMessage());
            return waypoints;
        }
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private GeoPoint pickupPoint(PickupInfo p) {
        if (p == null || p.getLatitude() == null || p.getLongitude() == null) return null;
        String label = p.getAddress() != null && !p.getAddress().isBlank() ? p.getAddress() : "Patient pickup";
        return new GeoPoint(p.getLatitude(), p.getLongitude(), label);
    }

    private GeoPoint destinationPoint(DestinationInfo d) {
        if (d == null || d.getLatitude() == null || d.getLongitude() == null) return null;
        String label = d.getHospitalName() != null && !d.getHospitalName().isBlank() ? d.getHospitalName()
                : (d.getAddress() != null && !d.getAddress().isBlank() ? d.getAddress() : "Hospital");
        return new GeoPoint(d.getLatitude(), d.getLongitude(), label);
    }
}
