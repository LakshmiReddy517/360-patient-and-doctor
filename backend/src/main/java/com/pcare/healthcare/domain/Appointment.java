package com.pcare.healthcare.domain;

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

import java.time.LocalDateTime;

/**
 * A doctor appointment (blueprint point 41). Optionally linked to a Case so the booking and its
 * outcome appear on the case timeline. Prescriptions/treatment are recorded by the authorised doctor.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointment", indexes = {
        @Index(name = "ix_appt_case", columnList = "caseId"),
        @Index(name = "ix_appt_doctor", columnList = "doctorId"),
        @Index(name = "ix_appt_status", columnList = "status")
})
public class Appointment extends BaseEntity {

    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    private Long patientId;

    @Column(length = 160)
    private String patientName;

    @Column(nullable = false)
    private Long doctorId;

    @Column(length = 160)
    private String doctorName;

    private Long hospitalId;

    @Column(length = 200)
    private String hospitalName;

    @Column(length = 120)
    private String department;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    @Column(length = 500)
    private String reason;

    /** Recorded by the authorised doctor during/after consultation. */
    @Column(length = 2000)
    private String consultationNotes;

    @Column(length = 2000)
    private String prescription;

    private LocalDateTime followUpAt;
}
