package com.pcare.patient.domain;

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

import java.time.LocalDate;

/**
 * A granular consent record (blueprint point 15): who can access what information, for what
 * purpose, from when until when — with approval/revocation and an audit trail (via BaseEntity).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consent", indexes = {
        @Index(name = "ix_consent_patient", columnList = "patientId")
})
public class Consent extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    /** What is being shared, e.g. "Medical records", "Lab reports", "Location". */
    @Column(nullable = false, length = 160)
    private String scope;

    /** Who may access it, e.g. "Assigned care agent", "Treating doctor", "Hospital partner". */
    @Column(nullable = false, length = 160)
    private String grantedTo;

    /** Why, e.g. "Coordinate hospital admission". */
    @Column(length = 240)
    private String purpose;

    private LocalDate validFrom;

    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentStatus status = ConsentStatus.GRANTED;
}
