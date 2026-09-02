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
 * Patient basic profile (blueprint point 11). Demographic and contact information plus optional
 * identity/travel/insurance fields. Clinical information lives in {@link MedicalProfile}, which is
 * kept separate so medical data can be access-controlled and consent-gated independently.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient", indexes = {
        @Index(name = "ix_patient_mobile", columnList = "mobile"),
        @Index(name = "ix_patient_name", columnList = "fullName")
})
public class Patient extends BaseEntity {

    /** Optional link to the login account (patient/guardian using the Patient App). */
    private Long userId;

    @Column(nullable = false, length = 160)
    private String fullName;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 30)
    private String mobile;

    @Column(length = 160)
    private String email;

    @Column(length = 400)
    private String addressLine;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(length = 80)
    private String country;

    @Column(length = 12)
    private String pincode;

    @Column(length = 120)
    private String emergencyContactName;

    @Column(length = 30)
    private String emergencyContactMobile;

    @Column(length = 60)
    private String emergencyContactRelation;

    @Column(length = 40)
    private String preferredLanguage;

    // Optional identity / travel / insurance
    @Column(length = 40)
    private String identityType;   // e.g. Aadhaar, Passport

    @Column(length = 60)
    private String identityNumber;

    @Column(length = 80)
    private String insuranceProvider;

    @Column(length = 60)
    private String insuranceNumber;
}
