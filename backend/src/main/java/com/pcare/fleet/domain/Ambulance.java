package com.pcare.fleet.domain;

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

import java.time.Instant;
import java.time.LocalDate;

/**
 * An ambulance in the fleet (blueprint point 24): registration, category/capability, owner,
 * compliance dates, assigned crew, current location, status, and readiness/equipment indicators.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ambulance", indexes = {
        @Index(name = "ux_ambulance_reg", columnList = "registrationNo", unique = true),
        @Index(name = "ix_ambulance_status", columnList = "status")
})
public class Ambulance extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, unique = true, length = 30)
    private String registrationNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AmbulanceCategory category = AmbulanceCategory.PATIENT_TRANSPORT;

    @Column(length = 160)
    private String ownerName;

    private LocalDate insuranceExpiry;
    private LocalDate fitnessExpiry;
    private LocalDate permitExpiry;

    /** Assigned crew (driver/EMT). */
    private Long driverId;

    @Column(length = 160)
    private String driverName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmbulanceStatus status = AmbulanceStatus.OFFLINE;

    private Double currentLatitude;
    private Double currentLongitude;
    private Instant locationUpdatedAt;

    // Equipment readiness indicators (blueprint point 27)
    @Column(nullable = false)
    private int oxygenLevelPercent = 100;

    @Column(nullable = false)
    private int fuelPercent = 100;

    @Column(nullable = false)
    private boolean ready = false;

    private Instant lastCheckAt;

    // Current trip linkage (for live tracking)
    private Long currentCaseId;

    @Column(length = 24)
    private String currentCaseNumber;

    @Column(length = 160)
    private String currentPatientName;
}
