package com.pcare.support.domain;

import com.pcare.common.domain.BaseEntity;
import com.pcare.support.domain.SupportEnums.IncidentSeverity;
import com.pcare.support.domain.SupportEnums.IncidentStatus;
import com.pcare.support.domain.SupportEnums.IncidentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** An operational incident (blueprint point 78) with escalation to the control room / emergency path. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "incident", indexes = {
        @Index(name = "ix_incident_case", columnList = "caseId"),
        @Index(name = "ix_incident_status", columnList = "status")
})
public class Incident extends BaseEntity {

    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSeverity severity = IncidentSeverity.MEDIUM;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(length = 1000)
    private String resolution;

    private Instant resolvedAt;
}
