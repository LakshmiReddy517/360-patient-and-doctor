package com.pcare.settlement.domain;

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
import java.time.Instant;

/** A settlement / payout batch for an agent or partner (blueprint points 45, 46, 72). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "settlement", indexes = {
        @Index(name = "ix_settlement_payee", columnList = "payeeType,payeeId"),
        @Index(name = "ix_settlement_status", columnList = "status")
})
public class Settlement extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayeeType payeeType;

    @Column(nullable = false)
    private Long payeeId;

    @Column(length = 160)
    private String payeeName;

    @Column(length = 40)
    private String periodLabel;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private int lineCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(length = 120)
    private String paymentReference;

    private Instant approvedAt;
    private Instant paidAt;

    public enum PayeeType { AGENT, CARETAKER, AMBULANCE_PARTNER, HOSPITAL_PARTNER }

    public enum SettlementStatus { PENDING, APPROVED, PAID, CANCELLED }
}
