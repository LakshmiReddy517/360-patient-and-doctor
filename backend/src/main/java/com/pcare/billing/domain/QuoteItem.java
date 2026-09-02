package com.pcare.billing.domain;

import com.pcare.servicerequest.domain.ServiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** A priced line on a quote: service, quantity, rate and computed amount. */
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class QuoteItem {

    @Enumerated(EnumType.STRING)
    @Column(name = "qi_service_type", length = 40)
    private ServiceType serviceType;

    @Column(name = "qi_label", nullable = false, length = 160)
    private String label;

    @Column(name = "qi_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "qi_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "qi_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
}
