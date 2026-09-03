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
import com.pcare.common.event.NotificationRequestedEvent;
import com.pcare.common.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher events;

    public CaseService(CaseFileRepository caseRepository,
                       CaseTimelineEventRepository timelineRepository,
                       CaseNumberGenerator numberGenerator,
                       com.pcare.live.LiveHub liveHub,
                       ApplicationEventPublisher events) {
        this.caseRepository = caseRepository;
        this.timelineRepository = timelineRepository;
        this.numberGenerator = numberGenerator;
        this.liveHub = liveHub;
        this.events = events;
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
        c.setSlaTargetAt(java.time.Instant.now().plus(slaMinutesFor(c.getPriority()), java.time.temporal.ChronoUnit.MINUTES));
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
        // Use the specific terminal event types so closure/completion notify the patient.
        CaseEventType evt = switch (req.status()) {
            case CLOSED -> CaseEventType.CASE_CLOSED;
            case COMPLETED -> CaseEventType.DISCHARGED;
            default -> CaseEventType.STATUS_CHANGED;
        };
        addEvent(id, evt, "Status: " + old + " -> " + req.status() + note, "CASE");
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
    /** First-response SLA target in minutes, tighter for higher priority (blueprint point 68). */
    private long slaMinutesFor(CasePriority priority) {
        return switch (priority) {
            case EMERGENCY -> 15;
            case HIGH -> 30;
            case NORMAL -> 120;
            case LOW -> 240;
        };
    }

    @Transactional
    public CaseTimelineEvent addEvent(Long caseId, CaseEventType type, String description, String source) {
        // Mark the first-response SLA as met the moment a resource is assigned to the case.
        if (type == CaseEventType.RESOURCE_ASSIGNED) {
            caseRepository.findById(caseId).ifPresent(c -> {
                if (c.getSlaMetAt() == null) {
                    c.setSlaMetAt(java.time.Instant.now());
                    caseRepository.save(c);
                }
            });
        }
        CaseTimelineEvent saved = timelineRepository.save(CaseTimelineEvent.of(caseId, type, description, source));
        liveHub.broadcast("event", java.util.Map.of(
                "caseId", caseId,
                "type", type.name(),
                "description", description,
                "source", source == null ? "" : source,
                "emergency", type == CaseEventType.EMERGENCY_ESCALATED,
                "at", java.time.Instant.now().toString()));
        notifyPatient(caseId, type, description);
        return saved;
    }

    /** Patient-facing message + channel/category for a timeline event, or null for internal events. */
    private record Notif(String channel, String category, String message) {}

    private Notif notificationFor(CaseEventType type, String description) {
        return switch (type) {
            case CASE_CREATED       -> new Notif("WHATSAPP", "GENERAL",   "Your care case has been created. We'll keep you updated at every step.");
            case TRIAGED            -> new Notif("WHATSAPP", "GENERAL",   "Your request is being reviewed by our care team.");
            case QUOTE_SENT         -> new Notif("WHATSAPP", "PAYMENT",   "A quote for your case is ready — please review and accept it in the app.");
            case QUOTE_ACCEPTED     -> new Notif("WHATSAPP", "GENERAL",   "Thank you — your quote has been accepted. We're arranging your services.");
            case RESOURCE_ASSIGNED  -> new Notif("WHATSAPP", "PICKUP",    "An agent has been assigned to your case and will reach you shortly.");
            case PICKUP_STARTED     -> new Notif("WHATSAPP", "PICKUP",    "Your agent is on the way to pick you up.");
            case PATIENT_PICKED     -> new Notif("WHATSAPP", "PICKUP",    "Pickup confirmed — you are on the way to the hospital.");
            case HOSPITAL_ARRIVED   -> new Notif("WHATSAPP", "PICKUP",    "You have arrived at the hospital.");
            case APPOINTMENT_BOOKED -> new Notif("WHATSAPP", "APPOINTMENT","Your doctor appointment has been booked.");
            case CARE_ACTIVITY      -> new Notif("WHATSAPP", "MEDICAL",   description == null || description.isBlank() ? "Update on your care." : description);
            case DISCHARGED         -> new Notif("WHATSAPP", "GENERAL",   "You have been discharged. Wishing you a speedy recovery.");
            case CASE_CLOSED        -> new Notif("WHATSAPP", "GENERAL",   "Your case is now complete. Thank you for choosing 360 Patient Care.");
            case EMERGENCY_ESCALATED-> new Notif("SMS",      "EMERGENCY", "EMERGENCY escalated — our team is arranging immediate help.");
            case COMPLAINT_RAISED   -> new Notif("WHATSAPP", "GENERAL",   "Your complaint has been registered. Our support team will get back to you.");
            // Internal / duplicate events (status/priority/notes/coordinator-assign/service-requested/quote-prepared,
            // and PAYMENT_RECEIVED which PaymentService already notifies) do not message the patient.
            default -> null;
        };
    }

    /** Fires a patient notification for notify-worthy case events, via the decoupled notification engine. */
    private void notifyPatient(Long caseId, CaseEventType type, String description) {
        Notif n = notificationFor(type, description);
        if (n == null) return;
        caseRepository.findById(caseId).ifPresent(c -> {
            if (c.getPatientMobile() == null && c.getPatientId() == null) return; // nowhere to deliver
            String subject = "360 Patient Care" + (c.getCaseNumber() != null ? " · " + c.getCaseNumber() : "");
            events.publishEvent(new NotificationRequestedEvent(
                    c.getPatientId(), c.getPatientName(), c.getPatientMobile(),
                    n.channel(), n.category(), subject, n.message(), caseId));
        });
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
                caseRepository.count(),
                caseRepository.countBySlaMetAtIsNullAndSlaTargetAtBefore(java.time.Instant.now()));
    }

    public CaseSummaryDto toSummary(CaseFile c) {
        return new CaseSummaryDto(c.getId(), c.getCaseNumber(), c.getPatientId(), c.getPatientName(),
                c.getPatientMobile(), c.getTitle(), c.getStatus(), c.getPriority(), c.isEmergency(),
                c.getAssignedToUserId(), c.getAssignedToName(), c.getSlaTargetAt(), c.getSlaMetAt(),
                isSlaBreached(c), c.getCreatedAt(), c.getUpdatedAt());
    }

    /** Breached when the first-response milestone landed (or, if still pending, is now) past the target. */
    private boolean isSlaBreached(CaseFile c) {
        if (c.getSlaTargetAt() == null) return false;
        java.time.Instant ref = c.getSlaMetAt() != null ? c.getSlaMetAt() : java.time.Instant.now();
        return ref.isAfter(c.getSlaTargetAt());
    }

    private TimelineEventDto toTimelineDto(CaseTimelineEvent e) {
        return new TimelineEventDto(e.getId(), e.getType(), e.getDescription(), e.getSource(),
                e.getCreatedBy(), e.getCreatedAt());
    }
}
