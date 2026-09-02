package com.pcare.settlement.domain;

import com.pcare.common.domain.BaseEntity;
import com.pcare.settlement.domain.Settlement.PayeeType;
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

/** An amount owed to an agent/partner for a completed service — a settlement line item. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "earning", indexes = {
        @Index(name = "ix_earning_payee", columnList = "payeeType,payeeId"),
        @Index(name = "ix_earning_settled", columnList = "settled")
})
public class Earning extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayeeType payeeType;

    @Column(nullable = false)
    private Long payeeId;

    @Column(length = 160)
    private String payeeName;

    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Column(length = 240)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean settled = false;

    private Long settlementId;
}
