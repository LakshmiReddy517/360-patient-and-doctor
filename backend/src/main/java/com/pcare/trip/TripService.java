package com.pcare.trip;

import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.dispatch.service.AssignmentService;
import com.pcare.dispatch.web.dto.DispatchDtos.AssignmentDto;
import com.pcare.fleet.service.FleetService;
import com.pcare.fleet.web.dto.FleetDtos.AmbulanceDto;
import com.pcare.servicerequest.service.ServiceRequestService;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.RequestDetailDto;
import com.pcare.trip.TripDtos.Trip;
import com.pcare.trip.TripDtos.TripLeg;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Assembles the unified {@link Trip} view for a case (blueprint points 44 & 76). */
@Service
public class TripService {

    private final CaseService caseService;
    private final ServiceRequestService serviceRequestService;
    private final AssignmentService assignmentService;
    private final FleetService fleetService;

    public TripService(CaseService caseService, ServiceRequestService serviceRequestService,
                       AssignmentService assignmentService, FleetService fleetService) {
        this.caseService = caseService;
        this.serviceRequestService = serviceRequestService;
        this.assignmentService = assignmentService;
        this.fleetService = fleetService;
    }

    @Transactional(readOnly = true)
    public Trip forCase(Long caseId) {
        CaseFile c = caseService.get(caseId);
        RequestDetailDto req = serviceRequestService.detailByCase(caseId);

        // Latest assignment for the case, if any.
        List<AssignmentDto> assignments = assignmentService.listByCase(caseId);
        AssignmentDto a = assignments.isEmpty() ? null : assignments.get(0);

        // Ambulance currently linked to this case, if any.
        AmbulanceDto amb = fleetService.listAll().stream()
                .filter(v -> caseId.equals(v.currentCaseId()))
                .findFirst().orElse(null);

        TripLeg pickup = null;
        TripLeg destination = null;
        Double distanceKm = null;
        java.time.LocalDateTime appointmentAt = null;
        if (req != null) {
            if (req.pickup() != null) {
                pickup = new TripLeg("Pickup", req.pickup().address(),
                        req.pickup().latitude(), req.pickup().longitude());
            }
            if (req.destination() != null) {
                String destLabel = req.destination().hospitalName() != null
                        ? req.destination().hospitalName() : req.destination().destinationType();
                destination = new TripLeg(destLabel != null ? destLabel : "Destination",
                        req.destination().address(), req.destination().latitude(), req.destination().longitude());
                appointmentAt = req.destination().appointmentAt();
            }
            distanceKm = legDistance(pickup, destination);
        }

        String phase = phase(a);

        return new Trip(
                c.getId(), c.getCaseNumber(), c.getPatientName(), phase,
                a != null ? a.agentId() : null,
                a != null ? a.agentName() : null,
                a != null ? a.status().name() : "UNASSIGNED",
                a != null ? a.pickupOtp() : null,
                a != null ? a.handoverOtp() : null,
                amb != null ? amb.id() : null,
                amb != null ? amb.registrationNo() : null,
                amb != null && amb.category() != null ? amb.category().name() : null,
                amb != null && amb.status() != null ? amb.status().name() : null,
                pickup, destination, distanceKm, appointmentAt,
                a != null ? a.acceptedAt() : null,
                a != null ? a.pickedAt() : null,
                a != null ? a.completedAt() : null);
    }

    /** Derives a coarse journey phase from the assignment's timings/status. */
    private String phase(AssignmentDto a) {
        if (a == null) return "NOT_STARTED";
        if (a.completedAt() != null) return "COMPLETED";
        if (a.pickedAt() != null) return "PATIENT_ONBOARD";
        if (a.acceptedAt() != null) return "EN_ROUTE_PICKUP";
        return "NOT_STARTED";
    }

    private Double legDistance(TripLeg from, TripLeg to) {
        if (from == null || to == null
                || from.latitude() == null || from.longitude() == null
                || to.latitude() == null || to.longitude() == null) {
            return null;
        }
        return Math.round(km(from.latitude(), from.longitude(), to.latitude(), to.longitude()) * 10.0) / 10.0;
    }

    private double km(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371, dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    }
}
