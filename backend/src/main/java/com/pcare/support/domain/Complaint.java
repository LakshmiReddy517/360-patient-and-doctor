package com.pcare.support.domain;

import com.pcare.common.domain.BaseEntity;
import com.pcare.support.domain.SupportEnums.ComplaintStatus;
import com.pcare.support.domain.SupportEnums.Priority;
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

/**
 * A complaint / grievance (blueprint point 77): creation, tracking, assignment, investigation,
 * resolution, reopening and closure, with configurable priority + SLA and full comment history.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "complaint", indexes = {
        @Index(name = "ix_complaint_case", columnList = "caseId"),
        @Index(name = "ix_complaint_status", columnList = "status")
})
public class Complaint extends BaseEntity {

    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Column(length = 160)
    private String patientName;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(length = 2000)
    private String description;

    @Column(length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    private Long assignedToUserId;

    @Column(length = 160)
    private String assignedToName;

    @Column(length = 2000)
    private String resolution;

    // SLA tracking
    private Instant slaResponseDueAt;
    private Instant slaResolutionDueAt;
    private Instant firstResponseAt;
    private Instant resolvedAt;

    @Column(nullable = false)
    private int reopenCount = 0;
}
