package com.pcare.servicerequest.service;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.domain.CasePriority;
import com.pcare.casefile.service.CaseService;
import com.pcare.casefile.web.dto.CaseDtos.CreateCaseRequest;
import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.dispatch.domain.Agent;
import com.pcare.dispatch.domain.AgentStatus;
import com.pcare.dispatch.repo.AgentRepository;
import com.pcare.dispatch.service.AssignmentService;
import com.pcare.servicerequest.domain.DestinationInfo;
import com.pcare.servicerequest.domain.PickupInfo;
import com.pcare.servicerequest.domain.RequestStatus;
import com.pcare.servicerequest.domain.ServiceRequest;
import com.pcare.servicerequest.repo.ServiceRequestRepository;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.CreateRequest;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.DestinationDto;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.PickupDto;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.RequestDetailDto;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.RequestStats;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.RequestSummaryDto;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.TransitionRequest;
import com.pcare.servicerequest.web.dto.ServiceRequestDtos.TriageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/** Service-request lifecycle, triage and conversion into a Case. */
@Service
public class ServiceRequestService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServiceRequestService.class);

    private final ServiceRequestRepository repository;
    private final CaseService caseService;
    private final com.pcare.live.LiveHub liveHub;
    private final AssignmentService assignmentService;
    private final AgentRepository agentRepository;
    private final com.pcare.healthcare.repo.HospitalRepository hospitalRepository;

    public ServiceRequestService(ServiceRequestRepository repository, CaseService caseService,
                                 com.pcare.live.LiveHub liveHub, AssignmentService assignmentService,
                                 AgentRepository agentRepository,
                                 com.pcare.healthcare.repo.HospitalRepository hospitalRepository) {
        this.repository = repository;
        this.caseService = caseService;
        this.liveHub = liveHub;
        this.assignmentService = assignmentService;
        this.agentRepository = agentRepository;
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional
    public ServiceRequest create(CreateRequest req) {
        ServiceRequest sr = new ServiceRequest();
        sr.setPatientId(req.patientId());
        sr.setPatientName(req.patientName());
        sr.setPatientMobile(req.patientMobile());
        sr.setServices(new HashSet<>(req.services()));
        sr.setNotes(req.notes());
        sr.setEmergency(req.emergency());
        sr.setStatus(req.emergency() ? RequestStatus.EMERGENCY_ESCALATED : RequestStatus.NEW);
        applyPickup(sr.getPickup(), req.pickup());
        applyDestination(sr.getDestination(), req.destination());
        ServiceRequest saved = repository.save(sr);
        liveHub.broadcast("request", java.util.Map.of(
                "id", saved.getId(), "patientName", saved.getPatientName() == null ? "" : saved.getPatientName(),
                "emergency", saved.isEmergency(), "status", saved.getStatus().name(),
                "services", saved.getServices().stream().map(Enum::name).toList()));
        return saved;
    }

    @Transactional(readOnly = true)
    public ServiceRequest get(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("ServiceRequest", id));
    }

    /** The originating service request for a case, as a detail DTO (pickup/destination), or null. */
    @Transactional(readOnly = true)
    public RequestDetailDto detailByCase(Long caseId) {
        return repository.findFirstByCaseId(caseId).map(this::toDetail).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<RequestSummaryDto> search(String q, RequestStatus status, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return repository.search(query, status, pageable).map(this::toSummary);
    }

    @Transactional
    public ServiceRequest triage(Long id, TriageRequest req) {
        ServiceRequest sr = get(id);
        sr.setEmergency(req.emergency());
        sr.setTriageNotes(req.triageNotes());
        sr.setStatus(req.emergency() ? RequestStatus.EMERGENCY_ESCALATED : RequestStatus.TRIAGE);
        repository.save(sr);
        if (sr.getCaseId() != null) {
            if (req.emergency()) {
                caseService.addEvent(sr.getCaseId(), CaseEventType.EMERGENCY_ESCALATED,
                        "Request triaged as EMERGENCY", "SERVICE_REQUEST");
            } else {
                caseService.addEvent(sr.getCaseId(), CaseEventType.TRIAGED, "Request triaged", "SERVICE_REQUEST");
            }
        }
        return sr;
    }

    @Transactional
    public ServiceRequest transition(Long id, TransitionRequest req) {
        ServiceRequest sr = get(id);
        if (!sr.getStatus().canTransitionTo(req.status())) {
            throw new BadRequestException("Illegal transition: " + sr.getStatus() + " -> " + req.status());
        }
        RequestStatus old = sr.getStatus();
        sr.setStatus(req.status());
        repository.save(sr);
        if (sr.getCaseId() != null) {
            String note = (req.note() == null || req.note().isBlank()) ? "" : " — " + req.note();
            caseService.addEvent(sr.getCaseId(), CaseEventType.STATUS_CHANGED,
                    "Request: " + old + " -> " + req.status() + note, "SERVICE_REQUEST");
        }
        return sr;
    }

    /** Converts a request into a Case (idempotent — returns existing link if already converted). */
    @Transactional
    public ServiceRequest convertToCase(Long id) {
        ServiceRequest sr = get(id);
        if (sr.getCaseId() != null) {
            return sr;
        }
        String title = sr.getServices().stream().map(Enum::name).collect(Collectors.joining(", "));
        CaseFile caseFile = caseService.createCase(new CreateCaseRequest(
                sr.getPatientId(), sr.getPatientName(), sr.getPatientMobile(),
                title, sr.getNotes(),
                sr.isEmergency() ? CasePriority.EMERGENCY : CasePriority.NORMAL,
                sr.isEmergency()));
        sr.setCaseId(caseFile.getId());
        sr.setCaseNumber(caseFile.getCaseNumber());
        if (!sr.getStatus().isTerminal() && sr.getStatus().ordinal() < RequestStatus.CONFIRMED.ordinal()) {
            sr.setStatus(RequestStatus.CONFIRMED);
        }
        repository.save(sr);
        caseService.addEvent(caseFile.getId(), CaseEventType.SERVICE_REQUESTED,
                "Created from service request #" + sr.getId() + " (" + title + ")", "SERVICE_REQUEST");
        // On approval, auto-dispatch to the best-matched available agent (nearest to pickup, then least-busy).
        autoDispatch(caseFile.getId(), title, sr);
        return sr;
    }

    /**
     * Picks the best dispatchable agent (ONLINE/AVAILABLE) scored by distance to the pickup location,
     * tie-broken by workload. No-op (with a case note) if none are free — an admin can then dispatch
     * manually. Failures here never abort the conversion.
     */
    private void autoDispatch(Long caseId, String title, ServiceRequest sr) {
        try {
            List<Agent> free = agentRepository.findByStatusIn(List.of(AgentStatus.ONLINE, AgentStatus.AVAILABLE));
            Double plat = sr.getPickup() != null ? sr.getPickup().getLatitude() : null;
            Double plng = sr.getPickup() != null ? sr.getPickup().getLongitude() : null;
            Agent chosen = free.stream().min(Comparator
                    .comparingDouble((Agent a) -> dispatchScore(a, plat, plng))
                    .thenComparingInt(Agent::getActiveAssignments)).orElse(null);
            if (chosen == null) {
                caseService.addEvent(caseId, CaseEventType.NOTE_ADDED,
                        "Approved — no agent available for auto-dispatch, awaiting manual assignment", "SERVICE_REQUEST");
                return;
            }
            String how = (plat != null && chosen.getCurrentLatitude() != null)
                    ? String.format("nearest agent (~%.1f km away)",
                            km(plat, plng, chosen.getCurrentLatitude(), chosen.getCurrentLongitude()))
                    : "least-busy available agent";
            assignmentService.assign(chosen.getId(), caseId, "Auto-dispatched on approval — " + how + " (" + title + ")");
        } catch (Exception e) {
            log.warn("Auto-dispatch failed for case {}: {}", caseId, e.getMessage());
        }
    }

    /** Distance (km) from the agent to the pickup; agents/pickups with unknown coords sort last. */
    private double dispatchScore(Agent a, Double plat, Double plng) {
        if (plat == null || plng == null || a.getCurrentLatitude() == null || a.getCurrentLongitude() == null) {
            return 1_000_000 + a.getActiveAssignments();
        }
        return km(plat, plng, a.getCurrentLatitude(), a.getCurrentLongitude());
    }

    private double km(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371, dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    }

    @Transactional(readOnly = true)
    public RequestStats stats() {
        return new RequestStats(
                repository.countByStatus(RequestStatus.NEW),
                repository.countByStatus(RequestStatus.UNDER_REVIEW),
                repository.countByStatus(RequestStatus.TRIAGE),
                repository.countByStatus(RequestStatus.CONFIRMED),
                repository.countByStatus(RequestStatus.IN_PROGRESS));
    }

    // ---- mapping ----
    private void applyPickup(PickupInfo p, PickupDto d) {
        if (d == null) return;
        p.setSource(d.source());
        p.setAddress(d.address());
        p.setLatitude(d.latitude());
        p.setLongitude(d.longitude());
        p.setScheduledAt(d.scheduledAt());
        p.setFlightOrTrainNumber(d.flightOrTrainNumber());
        p.setArrivalTime(d.arrivalTime());
        p.setTerminalOrCoach(d.terminalOrCoach());
        p.setSeatOrBerth(d.seatOrBerth());
    }

    private void applyDestination(DestinationInfo dest, DestinationDto d) {
        if (d == null) return;
        dest.setDestinationType(d.destinationType());
        dest.setHospitalId(d.hospitalId());
        dest.setHospitalName(d.hospitalName());
        dest.setDepartment(d.department());
        dest.setDoctorName(d.doctorName());
        dest.setAddress(d.address());
        dest.setLatitude(d.latitude());
        dest.setLongitude(d.longitude());
        dest.setAppointmentAt(d.appointmentAt());
        // Linked to the Hospital Master: when a hospitalId is given, fill name/coords/address from the master record.
        if (d.hospitalId() != null) {
            hospitalRepository.findById(d.hospitalId()).ifPresent(h -> {
                dest.setHospitalName(h.getName());
                if (h.getLatitude() != null) dest.setLatitude(h.getLatitude());
                if (h.getLongitude() != null) dest.setLongitude(h.getLongitude());
                if (dest.getAddress() == null || dest.getAddress().isBlank()) dest.setAddress(h.getAddressLine());
            });
        }
    }

    public RequestSummaryDto toSummary(ServiceRequest r) {
        return new RequestSummaryDto(r.getId(), r.getPatientId(), r.getPatientName(), r.getPatientMobile(),
                r.getServices(), r.getStatus(), r.isEmergency(), r.getCaseId(), r.getCaseNumber(), r.getCreatedAt());
    }

    public RequestDetailDto toDetail(ServiceRequest r) {
        // Hibernate returns null for an all-null @Embedded; fall back to empty objects for mapping.
        PickupInfo p = r.getPickup() != null ? r.getPickup() : new PickupInfo();
        DestinationInfo d = r.getDestination() != null ? r.getDestination() : new DestinationInfo();
        return new RequestDetailDto(r.getId(), r.getPatientId(), r.getPatientName(), r.getPatientMobile(),
                r.getServices(), r.getNotes(), r.getStatus(), r.isEmergency(), r.getTriageNotes(),
                r.getCaseId(), r.getCaseNumber(),
                new PickupDto(p.getSource(), p.getAddress(), p.getLatitude(), p.getLongitude(), p.getScheduledAt(),
                        p.getFlightOrTrainNumber(), p.getArrivalTime(), p.getTerminalOrCoach(), p.getSeatOrBerth()),
                new DestinationDto(d.getDestinationType(), d.getHospitalId(), d.getHospitalName(), d.getDepartment(),
                        d.getDoctorName(), d.getAddress(), d.getLatitude(), d.getLongitude(), d.getAppointmentAt()),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
