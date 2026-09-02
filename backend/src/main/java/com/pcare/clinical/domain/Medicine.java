package com.pcare.clinical.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A prescribed medicine for a patient (blueprint point 33). Recorded by the authorised clinician;
 * the platform stores and reminds — it does not diagnose or change medication.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medicine", indexes = {
        @Index(name = "ix_medicine_patient", columnList = "patientId"),
        @Index(name = "ix_medicine_case", columnList = "caseId")
})
public class Medicine extends BaseEntity {

    private Long patientId;
    private Long caseId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 60)
    private String dose;          // e.g. 500mg

    @Column(length = 60)
    private String route;         // Oral, IV, IM, etc.

    @Column(length = 60)
    private String frequency;     // e.g. 1-0-1

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(length = 80)
    private String foodInstruction;  // Before/After food

    @Column(length = 160)
    private String prescribedBy;

    @Column(length = 500)
    private String instructions;

    @Column(nullable = false)
    private boolean active = true;
}
