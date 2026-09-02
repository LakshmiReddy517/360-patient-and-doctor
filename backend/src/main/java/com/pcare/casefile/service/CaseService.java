package com.pcare.casefile.service;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.domain.CasePriority;
import com.pcare.casefile.domain.CaseStatus;
import com.pcare.casefile.domain.CaseTimelineEvent;
import com.pcare.casefile.repo.CaseFileRepository;
import com.pcare.casefile.repo.CaseTimelineEventRepository;
import com.pcare.casefile.web.dto.CaseDtos.AddNoteRequest;
import com.pcare.casefile.web.dto.CaseDtos.AssignRequest;
import com.pcare.casefile.web.dto.CaseDtos.CaseDetailDto;
import com.pcare.casefile.web.dto.CaseDtos.CaseSummaryDto;
import com.pcare.casefile.web.dto.CaseDtos.CreateCaseRequest;
import com.pcare.casefile.web.dto.CaseDtos.DashboardStats;
import com.pcare.casefile.web.dto.CaseDtos.TimelineEventDto;
import com.pcare.casefile.web.dto.CaseDtos.UpdatePriorityRequest;
import com.pcare.casefile.web.dto.CaseDtos.UpdateStatusRequest;
import com.pcare.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core Case operations. Creating a case, transitioning its lifecycle, assigning an owner and
 * recording notes — each of which writes a timeline event so the Case history stays complete.
 */
@Service
public class CaseService {

    private final CaseFileRepository caseRepository;
    private final CaseTimelineEventRepository timelineRepository;
    private final CaseNumberGenerator numberGenerator;
    private final com.pcare.live.LiveHub liveHub;

    public CaseService(CaseFileRepository caseRepository,
                       CaseTimelineEventRepository timelineRepository,
                       CaseNumberGenerator numberGenerator,
                       com.pcare.live.LiveHub liveHub) {
        this.caseRepository = caseRepository;
        this.timelineRepository = timelineRepository;
        this.numberGenerator = numberGenerator;
        this.liveHub = liveHub;
    }

    @Transactional
    public CaseFile createCase(CreateCaseRequest req) {
        CaseFile c = new CaseFile();
        c.setCaseNumber(numberGenerator.next());
        c.setPatientId(req.patientId());
        c.setPatientName(req.patientName());
        c.setPatientMobile(req.patientMobile());
        c.setTitle(req.title());
        c.setSummary(req.summary());
        c.setPriority(req.priority() != null ? req.priority() : CasePriority.NORMAL);
        c.setEmergency(req.emergency());
        if (req.emergency() && c.getPriority() != CasePriority.EMERGENCY) {
            c.setPriority(CasePriority.EMERGENCY);
        }
        c.setStatus(CaseStatus.OPEN);
        CaseFile saved = caseRepository.save(c);
        addEvent(saved.getId(), CaseEventType.CASE_CREATED,
                "Case " + saved.getCaseNumber() + " created for " + saved.getPatientName(), "CASE");
        if (saved.isEmergency()) {
            addEvent(saved.getId(), CaseEventType.EMERGENCY_ESCALATED,
                    "Case flagged EMERGENCY — escalation path engaged", "CASE");
        }
        return saved;
    }

    @Transactional
    public CaseFile updateStatus(Long id, UpdateStatusRequest req) {
        CaseFile c = get(id);
        CaseStatus old = c.getStatus();
        c.setStatus(req.status());
        caseRepository.save(c);
        String note = (req.note() == null || req.note().isBlank()) ? "" : " — " + req.note();
        addEvent(id, CaseEventType.STATUS_CHANGED,
                "Status: " + old + " -> " + req.status() + note, "CASE");
        return c;
    }

    @Transactional
    public CaseFile updatePriority(Long id, UpdatePriorityRequest req) {
        CaseFile c = get(id);
        CasePriority old = c.getPriority();
        c.setPriority(req.priority());
        c.setEmergency(req.priority() == CasePriority.EMERGENCY);
        caseRepository.save(c);
        addEvent(id, CaseEventType.PRIORITY_CHANGED, "Priority: " + old + " -> " + req.priority(), "CASE");
        return c;
    }

    @Transactional
    public CaseFile assign(Long id, AssignRequest req) {
        CaseFile c = get(id);
        c.setAssignedToUserId(req.userId());
        c.setAssignedToName(req.name());
        caseRepository.save(c);
        addEvent(id, CaseEventType.ASSIGNED, "Assigned to " + req.name(), "CASE");
        return c;
    }

    @Transactional
    public CaseFile addNote(Long id, AddNoteRequest req) {
        CaseFile c = get(id);
        addEvent(id, CaseEventType.NOTE_ADDED, req.note(), "CASE");
        return c;
    }

    /** Shared entry point used by other modules to append a timeline event to a case.
     * Also broadcasts the event to the real-time hub so the Command Centre updates live. */
    @Transactional
    public CaseTimelineEvent addEvent(Long caseId, CaseEventType type, String description, String source) {
        CaseTimelineEvent saved = timelineRepository.save(CaseTimelineEvent.of(caseId, type, description, source));
        liveHub.broadcast("event", java.util.Map.of(
                "caseId", caseId,
                "type", type.name(),
                "description", description,
                "source", source == null ? "" : source,
                "emergency", type == CaseEventType.EMERGENCY_ESCALATED,
                "at", java.time.Instant.now().toString()));
        return saved;
    }

    @Transactional(readOnly = true)
    public CaseFile get(Long id) {
        return caseRepository.findById(id).orElseThrow(() -> NotFoundException.of("Case", id));
    }

    @Transactional(readOnly = true)
    public CaseDetailDto getDetail(Long id) {
        CaseFile c = get(id);
        List<TimelineEventDto> timeline = timelineRepository.findByCaseIdOrderByCreatedAtDesc(id)
                .stream().map(this::toTimelineDto).toList();
        return new CaseDetailDto(toSummary(c), c.getSummary(), timeline);
    }

    @Transactional(readOnly = true)
    public Page<CaseSummaryDto> search(String q, CaseStatus status, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return caseRepository.search(query, status, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public DashboardStats dashboard() {
        return new DashboardStats(
                caseRepository.countByStatus(CaseStatus.OPEN),
                caseRepository.countByStatus(CaseStatus.IN_PROGRESS),
                caseRepository.countByStatus(CaseStatus.ON_HOLD),
                caseRepository.countByEmergencyTrueAndStatusNot(CaseStatus.CLOSED),
                caseRepository.countByStatus(CaseStatus.CLOSED),
                caseRepository.count());
    }

    public CaseSummaryDto toSummary(CaseFile c) {
        return new CaseSummaryDto(c.getId(), c.getCaseNumber(), c.getPatientId(), c.getPatientName(),
                c.getPatientMobile(), c.getTitle(), c.getStatus(), c.getPriority(), c.isEmergency(),
                c.getAssignedToUserId(), c.getAssignedToName(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private TimelineEventDto toTimelineDto(CaseTimelineEvent e) {
        return new TimelineEventDto(e.getId(), e.getType(), e.getDescription(), e.getSource(),
                e.getCreatedBy(), e.getCreatedAt());
    }
}
