package com.pcare.fleet.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Ambulance driver / Emergency Medical Technician (blueprint point 28). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "emt_driver", indexes = {
        @Index(name = "ix_emt_user", columnList = "userId")
})
public class EmtDriver extends BaseEntity {

    /** Optional link to a login account (Driver/EMT app). */
    private Long userId;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(length = 30)
    private String mobile;

    @Column(length = 40)
    private String licenceNumber;

    private LocalDate licenceExpiry;

    /** e.g. EMT-Basic, EMT-Paramedic. */
    @Column(length = 80)
    private String certification;

    private LocalDate certificationExpiry;

    @Column(nullable = false)
    private boolean medicalFitnessValid = true;

    @Column(nullable = false)
    private boolean backgroundVerified = false;

    @Column(length = 30)
    private String shift;

    @Column(nullable = false)
    private boolean onDuty = false;
}
