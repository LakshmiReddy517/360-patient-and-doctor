package com.pcare.clinical.domain;

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

/** A medication intake record (blueprint point 34): taken/missed, with adherence history. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medication_intake", indexes = {
        @Index(name = "ix_intake_medicine", columnList = "medicineId"),
        @Index(name = "ix_intake_case", columnList = "caseId")
})
public class MedicationIntake extends BaseEntity {

    @Column(nullable = false)
    private Long medicineId;

    private Long caseId;

    @Column(length = 160)
    private String medicineName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IntakeStatus status = IntakeStatus.TAKEN;

    @Column(length = 300)
    private String note;

    public enum IntakeStatus { TAKEN, MISSED }
}
