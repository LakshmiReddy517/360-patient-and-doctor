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

    public ServiceRequestService(ServiceRequestRepository repository, CaseService caseService,
                                 com.pcare.live.LiveHub liveHub, AssignmentService assignmentService,
                                 AgentRepository agentRepository) {
        this.repository = repository;
        this.caseService = caseService;
        this.liveHub = liveHub;
        this.assignmentService = assignmentService;
        this.agentRepository = agentRepository;
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
        // On approval, auto-dispatch the case to an available field agent so it appears at the
        // agent login immediately (least-busy ONLINE/AVAILABLE agent).
        autoDispatch(caseFile.getId(), title);
        return sr;
    }

    /**
     * Picks the least-busy dispatchable agent (ONLINE/AVAILABLE) and creates an assignment for the
     * case. No-op (with a case note) if none are free — an admin can then dispatch manually.
     * Failures here never abort the conversion.
     */
    private void autoDispatch(Long caseId, String title) {
        try {
            List<Agent> free = agentRepository.findByStatusIn(List.of(AgentStatus.ONLINE, AgentStatus.AVAILABLE));
            Agent chosen = free.stream().min(Comparator.comparingInt(Agent::getActiveAssignments)).orElse(null);
            if (chosen == null) {
                caseService.addEvent(caseId, CaseEventType.NOTE_ADDED,
                        "Approved — no agent available for auto-dispatch, awaiting manual assignment", "SERVICE_REQUEST");
                return;
            }
            assignmentService.assign(chosen.getId(), caseId, "Auto-dispatched on approval (" + title + ")");
        } catch (Exception e) {
            log.warn("Auto-dispatch failed for case {}: {}", caseId, e.getMessage());
        }
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
        dest.setHospitalName(d.hospitalName());
        dest.setDepartment(d.department());
        dest.setDoctorName(d.doctorName());
        dest.setAddress(d.address());
        dest.setAppointmentAt(d.appointmentAt());
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
                new DestinationDto(d.getDestinationType(), d.getHospitalName(), d.getDepartment(), d.getDoctorName(),
                        d.getAddress(), d.getAppointmentAt()),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
