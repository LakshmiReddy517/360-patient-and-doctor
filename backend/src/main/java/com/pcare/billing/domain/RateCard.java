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

    @Column(nullable = false)
    private boolean active = true;
}
