package com.pcare.support.service;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.common.exception.NotFoundException;
import com.pcare.support.domain.Incident;
import com.pcare.support.domain.SupportEnums.IncidentSeverity;
import com.pcare.support.domain.SupportEnums.IncidentStatus;
import com.pcare.support.repo.IncidentRepository;
import com.pcare.support.web.dto.SupportDtos.CreateIncidentRequest;
import com.pcare.support.web.dto.SupportDtos.IncidentDto;
import com.pcare.support.web.dto.SupportDtos.IncidentStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Operational incidents (blueprint point 78) and their escalation to the control room. */
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final CaseService caseService;

    public IncidentService(IncidentRepository incidentRepository, CaseService caseService) {
        this.incidentRepository = incidentRepository;
        this.caseService = caseService;
    }

    @Transactional
    public Incident create(CreateIncidentRequest req) {
        Incident i = new Incident();
        i.setType(req.type());
        i.setSeverity(req.severity() != null ? req.severity() : IncidentSeverity.MEDIUM);
        i.setDescription(req.description());
        i.setStatus(IncidentStatus.OPEN);
        if (req.caseId() != null) {
            CaseFile caseFile = caseService.get(req.caseId());
            i.setCaseId(caseFile.getId());
            i.setCaseNumber(caseFile.getCaseNumber());
        }
        Incident saved = incidentRepository.save(i);
        if (saved.getCaseId() != null) {
            CaseEventType eventType = saved.getSeverity() == IncidentSeverity.CRITICAL
                    ? CaseEventType.EMERGENCY_ESCALATED : CaseEventType.NOTE_ADDED;
            caseService.addEvent(saved.getCaseId(), eventType,
                    "Incident [" + saved.getSeverity() + "] " + saved.getType() + ": " + saved.getDescription(),
                    "INCIDENT");
        }
        return saved;
    }

    @Transactional
    public Incident updateStatus(Long id, IncidentStatus status, String resolution) {
        Incident i = get(id);
        i.setStatus(status);
        if (resolution != null && !resolution.isBlank()) {
            i.setResolution(resolution);
        }
        if (status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
            i.setResolvedAt(Instant.now());
        }
        incidentRepository.save(i);
        if (i.getCaseId() != null && status == IncidentStatus.ESCALATED) {
            caseService.addEvent(i.getCaseId(), CaseEventType.EMERGENCY_ESCALATED,
                    "Incident #" + i.getId() + " escalated to control room / emergency path", "INCIDENT");
        }
        return i;
    }

    @Transactional(readOnly = true)
    public Incident get(Long id) {
        return incidentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Incident", id));
    }

    @Transactional(readOnly = true)
    public Page<IncidentDto> search(IncidentStatus status, Pageable pageable) {
        return incidentRepository.search(status, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public IncidentStats stats() {
        return new IncidentStats(
                incidentRepository.countByStatus(IncidentStatus.OPEN),
                incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS),
                incidentRepository.countByStatus(IncidentStatus.ESCALATED),
                incidentRepository.countByStatus(IncidentStatus.RESOLVED));
    }

    public IncidentDto toDto(Incident i) {
        return new IncidentDto(i.getId(), i.getCaseId(), i.getCaseNumber(), i.getType(), i.getSeverity(),
                i.getDescription(), i.getStatus(), i.getResolution(), i.getResolvedAt(), i.getCreatedBy(),
                i.getCreatedAt());
    }
}
