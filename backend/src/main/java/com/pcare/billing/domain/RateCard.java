package com.pcare.billing.domain;

import com.pcare.common.domain.BaseEntity;
import com.pcare.servicerequest.domain.ServiceType;
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

/**
 * A configurable rate for a service type — the backbone of the rule-based pricing engine
 * (blueprint point 17). Rates are data, not hard-coded into screens, so pricing can be tuned
 * without code changes.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rate_card", indexes = {
        @Index(name = "ux_rate_service", columnList = "serviceType", unique = true)
})
public class RateCard extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ServiceType serviceType;

    @Column(nullable = false, length = 120)
    private String label;

    /** Unit of charge, e.g. "per trip", "per day", "per hour", "per km". */
    @Column(nullable = false, length = 40)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitRate = BigDecimal.ZERO;

    // ---- Multi-factor pricing inputs (blueprint point 17) ----
    /** Flat pickup / base fare added once per trip, independent of distance. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare = BigDecimal.ZERO;

    /** Charge added per kilometre of the trip (pickup -> destination). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal perKmRate = BigDecimal.ZERO;

    /** Multiplier applied to (base + distance) when the case is an emergency, e.g. 1.50 = +50%. */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal emergencyMultiplier = BigDecimal.ONE;

    /** Percentage surcharge added for night-hour trips (22:00–06:00), e.g. 20 = +20%. */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal nightSurchargePercent = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;
}
