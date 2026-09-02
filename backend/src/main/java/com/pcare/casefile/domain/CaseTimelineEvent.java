package com.pcare.casefile.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One chronological event on a Case's timeline. Every significant action across all modules
 * (registration, upload, request, quote, payment, assignment, pickup, arrival, appointment,
 * care activity, discharge, closure) writes one of these — the single auditable case history.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "case_timeline_event", indexes = {
        @Index(name = "ix_timeline_case", columnList = "caseId")
})
public class CaseTimelineEvent extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CaseEventType type;

    @Column(nullable = false, length = 500)
    private String description;

    /** Optional module/source that produced the event, e.g. "SERVICE_REQUEST", "PAYMENT". */
    @Column(length = 40)
    private String source;

    public static CaseTimelineEvent of(Long caseId, CaseEventType type, String description, String source) {
        CaseTimelineEvent e = new CaseTimelineEvent();
        e.caseId = caseId;
        e.type = type;
        e.description = description;
        e.source = source;
        return e;
    }
}
