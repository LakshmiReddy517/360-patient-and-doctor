package com.pcare.patient.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Patient medical profile (blueprint point 12). The platform stores information as provided;
 * clinical decisions remain with qualified clinicians. Access is consent-gated and least-privilege.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medical_profile", indexes = {
        @Index(name = "ux_medical_patient", columnList = "patientId", unique = true)
})
public class MedicalProfile extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    @Column(length = 1000)
    private String currentProblem;

    @Column(length = 2000)
    private String diagnosisHistory;

    @Column(length = 1000)
    private String chronicDiseases;

    @Column(length = 1000)
    private String surgeries;

    @Column(length = 1000)
    private String previousHospitalisations;

    @Column(length = 1000)
    private String allergies;

    @Column(length = 1000)
    private String currentMedicines;

    @Column(length = 10)
    private String bloodGroup;

    @Column(length = 500)
    private String disabilityOrMobility;

    @Column(length = 2000)
    private String relevantHistory;
}
