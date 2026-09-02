package com.pcare.care.domain;

import com.pcare.care.domain.CareEnums.CareStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A caretaker assigned to a patient/case (blueprint point 36). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "caretaker_assignment", indexes = {
        @Index(name = "ix_ctassign_case", columnList = "caseId"),
        @Index(name = "ix_ctassign_caretaker", columnList = "caretakerId")
})
public class CaretakerAssignment extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Column(length = 160)
    private String patientName;

    @Column(nullable = false)
    private Long caretakerId;

    @Column(length = 160)
    private String caretakerName;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @Column(length = 30)
    private String shift;

    @Column(precision = 10, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(length = 500)
    private String responsibilities;

    @Column(nullable = false)
    private boolean mealsProvided = false;

    @Column(nullable = false)
    private boolean accommodationProvided = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareStatus status = CareStatus.REQUESTED;
}
