package com.pcare.billing.cash;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cash physically collected by a field agent from a patient on a trip (blueprint point 46).
 * Tracks cash-in-hand until the agent deposits it, so collections reconcile against deposits.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cash_collection", indexes = {
        @Index(name = "ix_cash_agent", columnList = "agentId"),
        @Index(name = "ix_cash_case", columnList = "caseId"),
        @Index(name = "ix_cash_deposited", columnList = "deposited")
})
public class CashCollection extends BaseEntity {

    @Column(nullable = false)
    private Long agentId;

    @Column(length = 160)
    private String agentName;

    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 300)
    private String note;

    @Column(nullable = false)
    private boolean deposited = false;

    private Instant depositedAt;

    @Column(length = 120)
    private String depositReference;
}
