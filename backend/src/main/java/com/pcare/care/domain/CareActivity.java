package com.pcare.care.domain;

import com.pcare.care.domain.CareEnums.CareActivityType;
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
 * One entry in the caretaker daily activity diary (blueprint point 37): medicine assistance, meals,
 * doctor visits, vitals, rest, observations and handover notes — each timestamped and attributed
 * (recorded-by/at come from BaseEntity audit metadata).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "care_activity", indexes = {
        @Index(name = "ix_activity_assignment", columnList = "assignmentId"),
        @Index(name = "ix_activity_case", columnList = "caseId")
})
public class CareActivity extends BaseEntity {

    @Column(nullable = false)
    private Long assignmentId;

    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareActivityType type;

    @Column(nullable = false, length = 500)
    private String note;
}
