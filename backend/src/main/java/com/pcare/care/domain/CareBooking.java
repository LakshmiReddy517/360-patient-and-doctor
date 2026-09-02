package com.pcare.care.domain;

import com.pcare.care.domain.CareEnums.CareServiceType;
import com.pcare.care.domain.CareEnums.CareStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A unified case-linked care booking covering Accommodation (point 42), Food (point 43) and
 * Local Transport (point 44). Type-specific fields are optional and used per {@link #type}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "care_booking", indexes = {
        @Index(name = "ix_carebooking_case", columnList = "caseId"),
        @Index(name = "ix_carebooking_type", columnList = "type")
})
public class CareBooking extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Column(length = 160)
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareServiceType type;

    /** Provider — hotel/guest house, kitchen/caterer, or transport operator. */
    @Column(length = 200)
    private String provider;

    @Column(length = 500)
    private String description;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitRate = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Accommodation
    @Column(length = 60)
    private String roomType;

    // Food
    @Column(length = 60)
    private String dietType;

    @Column(length = 40)
    private String meal;

    // Local transport
    @Column(length = 60)
    private String tripType;

    @Column(length = 240)
    private String pickup;

    @Column(length = 240)
    private String destination;

    private Double distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareStatus status = CareStatus.REQUESTED;
}
