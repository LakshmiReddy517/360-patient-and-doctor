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

/** A reusable, configurable line inside a package. */
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class PackageItem {

    @Enumerated(EnumType.STRING)
    @Column(name = "item_service_type", length = 40)
    private ServiceType serviceType;

    @Column(name = "item_label", length = 160)
    private String label;

    @Column(name = "item_quantity", precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "item_unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;
}
