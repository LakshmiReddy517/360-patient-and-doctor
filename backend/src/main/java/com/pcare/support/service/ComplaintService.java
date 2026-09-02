package com.pcare.support.service;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.common.exception.NotFoundException;
import com.pcare.support.domain.Complaint;
import com.pcare.support.domain.ComplaintComment;
import com.pcare.support.domain.SlaPolicy;
import com.pcare.support.domain.SupportEnums.ComplaintStatus;
import com.pcare.support.domain.SupportEnums.Priority;
import com.pcare.support.repo.ComplaintCommentRepository;
import com.pcare.support.repo.ComplaintRepository;
import com.pcare.support.repo.SlaPolicyRepository;
import com.pcare.support.web.dto.SupportDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Complaint lifecycle, comment history and SLA (configurable per priority). */
@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintCommentRepository commentRepository;
    private final SlaPolicyRepository slaRepository;
    private final CaseService caseService;

    public ComplaintService(ComplaintRepository complaintRepository, ComplaintCommentRepository commentRepository,
                            SlaPolicyRepository slaRepository, CaseService caseService) {
        this.complaintRepository = complaintRepository;
        this.commentRepository = commentRepository;
        this.slaRepository = slaRepository;
        this.caseService = caseService;
    }

    @Transactional
    public Complaint create(CreateComplaintRequest req) {
        Complaint c = new Complaint();
        c.setSubject(req.subject());
        c.setDescription(req.description());
        c.setCategory(req.category());
        c.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        c.setPatientName(req.patientName());
        c.setStatus(ComplaintStatus.OPEN);

        if (req.caseId() != null) {
            CaseFile caseFile = caseService.get(req.caseId());
            c.setCaseId(caseFile.getId());
            c.setCaseNumber(caseFile.getCaseNumber());
            if (c.getPatientName() == null) c.setPatientName(caseFile.getPatientName());
        }

        Instant now = Instant.now();
        SlaPolicy sla = slaFor(c.getPriority());
        c.setSlaResponseDueAt(now.plus(sla.getResponseMinutes(), ChronoUnit.MINUTES));
        c.setSlaResolutionDueAt(now.plus(sla.getResolutionMinutes(), ChronoUnit.MINUTES));

        Complaint saved = complaintRepository.save(c);
        if (saved.getCaseId() != null) {
            caseService.addEvent(saved.getCaseId(), CaseEventType.COMPLAINT_RAISED,
                    "Complaint #" + saved.getId() + ": " + saved.getSubject(), "SUPPORT");
        }
        return saved;
    }

    @Transactional
    public Complaint assign(Long id, AssignComplaintRequest req) {
        Complaint c = get(id);
        c.setAssignedToUserId(req.userId());
        c.setAssignedToName(req.name());
        if (c.getStatus() == ComplaintStatus.OPEN) c.setStatus(ComplaintStatus.ASSIGNED);
        markResponded(c);
        return complaintRepository.save(c);
    }

    @Transactional
    public Complaint setStatus(Long id, ComplaintStatus status, String note) {
        Complaint c = get(id);
        if (status == ComplaintStatus.REOPENED) {
            c.setReopenCount(c.getReopenCount() + 1);
            c.setResolvedAt(null);
        }
        if (status == ComplaintStatus.RESOLVED && c.getResolvedAt() == null) {
            c.setResolvedAt(Instant.now());
        }
        if (status == ComplaintStatus.INVESTIGATING) {
            markResponded(c);
        }
        c.setStatus(status);
        if (note != null && !note.isBlank()) {
            addCommentInternal(c.getId(), note, true);
        }
        return complaintRepository.save(c);
    }

    @Transactional
    public Complaint resolve(Long id, ResolveComplaintRequest req) {
        Complaint c = get(id);
        c.setResolution(req.resolution());
        c.setStatus(ComplaintStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        markResponded(c);
        return complaintRepository.save(c);
    }

    @Transactional
    public ComplaintComment addComment(Long complaintId, AddCommentRequest req) {
        Complaint c = get(complaintId);
        if (!req.internal()) {
            markResponded(c);
            complaintRepository.save(c);
        }
        return addCommentInternal(complaintId, req.message(), req.internal());
    }

    private ComplaintComment addCommentInternal(Long complaintId, String message, boolean internal) {
        ComplaintComment cc = new ComplaintComment();
        cc.setComplaintId(complaintId);
        cc.setMessage(message);
        cc.setInternal(internal);
        return commentRepository.save(cc);
    }

    private void markResponded(Complaint c) {
        if (c.getFirstResponseAt() == null) {
            c.setFirstResponseAt(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public Complaint get(Long id) {
        return complaintRepository.findById(id).orElseThrow(() -> NotFoundException.of("Complaint", id));
    }

    @Transactional(readOnly = true)
    public ComplaintDto getDetail(Long id) {
        Complaint c = get(id);
        List<CommentDto> comments = commentRepository.findByComplaintIdOrderByCreatedAtAsc(id)
                .stream().map(this::toCommentDto).toList();
        return toDto(c, comments);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintDto> search(String q, ComplaintStatus status, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return complaintRepository.search(query, status, pageable).map(c -> toDto(c, List.of()));
    }

    @Transactional(readOnly = true)
    public ComplaintStats stats() {
        return new ComplaintStats(
                complaintRepository.countByStatus(ComplaintStatus.OPEN)
                        + complaintRepository.countByStatus(ComplaintStatus.ASSIGNED)
                        + complaintRepository.countByStatus(ComplaintStatus.REOPENED),
                complaintRepository.countByStatus(ComplaintStatus.INVESTIGATING),
                complaintRepository.countByStatus(ComplaintStatus.RESOLVED),
                complaintRepository.countResolutionBreached(Instant.now()));
    }

    // ---- SLA policy ----
    @Transactional(readOnly = true)
    public List<SlaPolicyDto> slaPolicies() {
        return slaRepository.findAll().stream()
                .map(p -> new SlaPolicyDto(p.getId(), p.getPriority(), p.getResponseMinutes(), p.getResolutionMinutes()))
                .toList();
    }

    @Transactional
    public SlaPolicyDto upsertSla(UpsertSlaPolicyRequest req) {
        SlaPolicy p = slaRepository.findByPriority(req.priority()).orElseGet(SlaPolicy::new);
        p.setPriority(req.priority());
        p.setResponseMinutes(req.responseMinutes());
        p.setResolutionMinutes(req.resolutionMinutes());
        SlaPolicy saved = slaRepository.save(p);
        return new SlaPolicyDto(saved.getId(), saved.getPriority(), saved.getResponseMinutes(), saved.getResolutionMinutes());
    }

    private SlaPolicy slaFor(Priority priority) {
        return slaRepository.findByPriority(priority).orElseGet(() -> {
            SlaPolicy def = new SlaPolicy();
            def.setPriority(priority);
            def.setResponseMinutes(240);
            def.setResolutionMinutes(2880);
            return def;
        });
    }

    // ---- mapping ----
    private ComplaintDto toDto(Complaint c, List<CommentDto> comments) {
        Instant now = Instant.now();
        boolean closed = c.getStatus() == ComplaintStatus.CLOSED;
        boolean responseBreached = c.getFirstResponseAt() == null && c.getSlaResponseDueAt() != null
                && now.isAfter(c.getSlaResponseDueAt()) && !closed;
        boolean resolutionBreached = c.getResolvedAt() == null && c.getSlaResolutionDueAt() != null
                && now.isAfter(c.getSlaResolutionDueAt()) && !closed;
        return new ComplaintDto(c.getId(), c.getCaseId(), c.getCaseNumber(), c.getPatientName(), c.getSubject(),
                c.getDescription(), c.getCategory(), c.getPriority(), c.getStatus(), c.getAssignedToUserId(),
                c.getAssignedToName(), c.getResolution(), c.getSlaResponseDueAt(), c.getSlaResolutionDueAt(),
                c.getFirstResponseAt(), c.getResolvedAt(), responseBreached, resolutionBreached, c.getReopenCount(),
                c.getCreatedAt(), comments);
    }

    private CommentDto toCommentDto(ComplaintComment cc) {
        return new CommentDto(cc.getId(), cc.getComplaintId(), cc.getMessage(), cc.isInternal(),
                cc.getCreatedBy(), cc.getCreatedAt());
    }
}
