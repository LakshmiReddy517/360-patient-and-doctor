package com.pcare.clinical.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A patient condition / vitals reading (blueprint point 38). Audit metadata (recorded-by/at) is
 * populated automatically by BaseEntity — never left blank.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient_condition", indexes = {
        @Index(name = "ix_condition_patient", columnList = "patientId"),
        @Index(name = "ix_condition_case", columnList = "caseId")
})
public class PatientCondition extends BaseEntity {

    private Long patientId;
    private Long caseId;

    @Column(name = "condition_text", length = 200)
    private String condition;

    @Column(length = 10)
    private String temperature;   // e.g. 98.6F

    @Column(length = 20)
    private String bloodPressure; // e.g. 120/80

    @Column(length = 10)
    private String pulse;

    @Column(length = 10)
    private String spo2;

    private Integer painScore;    // 0-10

    @Column(length = 500)
    private String notes;
}
