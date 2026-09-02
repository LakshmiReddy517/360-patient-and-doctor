package com.pcare.healthcare.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** A doctor/clinician (blueprint points 31, 32). Clinical actions are performed by the doctor;
 * the platform coordinates appointments and records. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctor", indexes = {
        @Index(name = "ix_doctor_hospital", columnList = "hospitalId"),
        @Index(name = "ix_doctor_specialty", columnList = "specialty")
})
public class Doctor extends BaseEntity {

    /** Optional link to a login account (Doctor app/portal). */
    private Long userId;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(length = 120)
    private String specialty;

    @Column(length = 120)
    private String department;

    private Long hospitalId;

    @Column(length = 200)
    private String hospitalName;

    @Column(length = 160)
    private String qualification;

    @Column(length = 60)
    private String registrationNumber;

    @Column(length = 30)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean available = true;
}
