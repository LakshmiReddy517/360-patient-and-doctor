package com.pcare.dispatch.domain;

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

import java.time.Instant;

/**
 * The assignment of an agent to a case, tracked through the pickup workflow (blueprint point 23)
 * with an OTP handshake at pickup and hospital handover.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assignment", indexes = {
        @Index(name = "ix_assignment_case", columnList = "caseId"),
        @Index(name = "ix_assignment_agent", columnList = "agentId"),
        @Index(name = "ix_assignment_status", columnList = "status")
})
public class Assignment extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Column(length = 160)
    private String patientName;

    @Column(nullable = false)
    private Long agentId;

    @Column(length = 160)
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AssignmentStatus status = AssignmentStatus.OFFERED;

    /** OTP the patient shows the agent to confirm pickup. */
    @Column(length = 6)
    private String pickupOtp;

    /** OTP confirming handover at the hospital. */
    @Column(length = 6)
    private String handoverOtp;

    @Column(length = 500)
    private String notes;

    private Instant acceptedAt;
    private Instant pickedAt;
    private Instant completedAt;
}
