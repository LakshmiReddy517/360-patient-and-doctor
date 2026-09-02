package com.pcare.billing.domain;

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

/**
 * A recorded financial transaction against a quote/case (blueprint points 45, 46). Advances,
 * balance payments and refunds are all auditable via BaseEntity's created-by/at metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment", indexes = {
        @Index(name = "ix_payment_case", columnList = "caseId"),
        @Index(name = "ix_payment_quote", columnList = "quoteId")
})
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    private Long quoteId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType type = PaymentType.ADVANCE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.CASH;

    @Column(length = 120)
    private String reference;

    @Column(length = 300)
    private String note;

    public enum PaymentType { ADVANCE, BALANCE, REFUND }

    public enum PaymentMethod { CASH, UPI, CARD, BANK_TRANSFER, WALLET }
}
