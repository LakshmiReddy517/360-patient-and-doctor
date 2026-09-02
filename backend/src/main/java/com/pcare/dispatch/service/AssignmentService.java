package com.pcare.dispatch.service;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.dispatch.domain.Agent;
import com.pcare.dispatch.domain.AgentStatus;
import com.pcare.dispatch.domain.Assignment;
import com.pcare.dispatch.domain.AssignmentStatus;
import com.pcare.dispatch.repo.AgentRepository;
import com.pcare.dispatch.repo.AssignmentRepository;
import com.pcare.dispatch.web.dto.DispatchDtos.AssignmentDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

/** Dispatch: assigning agents to cases and driving the pickup/handover workflow (points 21, 23). */
@Service
public class AssignmentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AssignmentRepository assignmentRepository;
    private final AgentRepository agentRepository;
    private final CaseService caseService;

    public AssignmentService(AssignmentRepository assignmentRepository, AgentRepository agentRepository,
                             CaseService caseService) {
        this.assignmentRepository = assignmentRepository;
        this.agentRepository = agentRepository;
        this.caseService = caseService;
    }

    @Transactional
    public Assignment assign(Long agentId, Long caseId, String notes) {
        Agent agent = agentRepository.findById(agentId).orElseThrow(() -> NotFoundException.of("Agent", agentId));
        CaseFile caseFile = caseService.get(caseId);

        Assignment a = new Assignment();
        a.setCaseId(caseId);
        a.setCaseNumber(caseFile.getCaseNumber());
        a.setPatientName(caseFile.getPatientName());
        a.setAgentId(agentId);
        a.setAgentName(agent.getFullName());
        a.setStatus(AssignmentStatus.OFFERED);
        a.setNotes(notes);
        a.setPickupOtp(otp());
        a.setHandoverOtp(otp());
        Assignment saved = assignmentRepository.save(a);

        agent.setStatus(AgentStatus.JOB_OFFERED);
        agent.setActiveAssignments(agent.getActiveAssignments() + 1);
        agentRepository.save(agent);

        caseService.addEvent(caseId, CaseEventType.RESOURCE_ASSIGNED,
                "Agent " + agent.getFullName() + " assigned (assignment #" + saved.getId() + ")", "DISPATCH");
        return saved;
    }

    @Transactional
    public Assignment accept(Long id) {
        Assignment a = get(id);
        a.setStatus(AssignmentStatus.ACCEPTED);
        a.setAcceptedAt(Instant.now());
        assignmentRepository.save(a);
        syncAgent(a.getAgentId(), AgentStatus.ACCEPTED);
        return a;
    }

    @Transactional
    public Assignment updateStatus(Long id, AssignmentStatus status, String note) {
        Assignment a = get(id);
        if (a.getStatus().isClosed()) {
            throw new BadRequestException("Assignment is already " + a.getStatus());
        }
        a.setStatus(status);
        assignmentRepository.save(a);

        switch (status) {
            case EN_ROUTE -> {
                syncAgent(a.getAgentId(), AgentStatus.EN_ROUTE);
                caseService.addEvent(a.getCaseId(), CaseEventType.PICKUP_STARTED,
                        "Agent en route to pickup", "DISPATCH");
            }
            case ARRIVED_AT_PICKUP -> syncAgent(a.getAgentId(), AgentStatus.ARRIVED);
            case IN_TRANSIT -> syncAgent(a.getAgentId(), AgentStatus.IN_TRANSIT);
            case HOSPITAL_ARRIVED -> {
                syncAgent(a.getAgentId(), AgentStatus.HOSPITAL_ARRIVED);
                caseService.addEvent(a.getCaseId(), CaseEventType.HOSPITAL_ARRIVED,
                        "Arrived at hospital", "DISPATCH");
            }
            default -> { /* no side effects */ }
        }
        return a;
    }

    /** Patient shows OTP; agent confirms pickup. */
    @Transactional
    public Assignment verifyPickup(Long id, String otp) {
        Assignment a = get(id);
        if (!a.getPickupOtp().equals(otp.trim())) {
            throw new BadRequestException("Incorrect pickup OTP");
        }
        a.setStatus(AssignmentStatus.PATIENT_PICKED);
        a.setPickedAt(Instant.now());
        assignmentRepository.save(a);
        syncAgent(a.getAgentId(), AgentStatus.PATIENT_PICKED);
        caseService.addEvent(a.getCaseId(), CaseEventType.PATIENT_PICKED,
                "Patient picked up (OTP verified)", "DISPATCH");
        return a;
    }

    /** Handover OTP confirms completion at the hospital. */
    @Transactional
    public Assignment verifyHandover(Long id, String otp) {
        Assignment a = get(id);
        if (!a.getHandoverOtp().equals(otp.trim())) {
            throw new BadRequestException("Incorrect handover OTP");
        }
        a.setStatus(AssignmentStatus.COMPLETED);
        a.setCompletedAt(Instant.now());
        assignmentRepository.save(a);
        releaseAgent(a.getAgentId());
        caseService.addEvent(a.getCaseId(), CaseEventType.CARE_ACTIVITY,
                "Patient handed over at hospital — assignment completed", "DISPATCH");
        return a;
    }

    @Transactional
    public Assignment cancel(Long id, String note) {
        Assignment a = get(id);
        a.setStatus(AssignmentStatus.CANCELLED);
        a.setNotes(note);
        assignmentRepository.save(a);
        releaseAgent(a.getAgentId());
        caseService.addEvent(a.getCaseId(), CaseEventType.NOTE_ADDED,
                "Assignment #" + a.getId() + " cancelled" + (note != null ? ": " + note : ""), "DISPATCH");
        return a;
    }

    @Transactional(readOnly = true)
    public Assignment get(Long id) {
        return assignmentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Assignment", id));
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto> listByCase(Long caseId) {
        return assignmentRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto> listByAgent(Long agentId) {
        return assignmentRepository.findByAgentIdOrderByCreatedAtDesc(agentId).stream().map(this::toDto).toList();
    }

    private void syncAgent(Long agentId, AgentStatus status) {
        agentRepository.findById(agentId).ifPresent(agent -> {
            agent.setStatus(status);
            agentRepository.save(agent);
        });
    }

    private void releaseAgent(Long agentId) {
        agentRepository.findById(agentId).ifPresent(agent -> {
            agent.setActiveAssignments(Math.max(0, agent.getActiveAssignments() - 1));
            agent.setStatus(agent.getActiveAssignments() == 0 ? AgentStatus.AVAILABLE : agent.getStatus());
            agentRepository.save(agent);
        });
    }

    private String otp() {
        return String.format("%04d", RANDOM.nextInt(10000));
    }

    public AssignmentDto toDto(Assignment a) {
        return new AssignmentDto(a.getId(), a.getCaseId(), a.getCaseNumber(), a.getPatientName(),
                a.getAgentId(), a.getAgentName(), a.getStatus(), a.getPickupOtp(), a.getHandoverOtp(),
                a.getNotes(), a.getAcceptedAt(), a.getPickedAt(), a.getCompletedAt(), a.getCreatedAt());
    }
}
