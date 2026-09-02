package com.pcare.billing.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A quote for a case (blueprint point 18): services, quantities, rates, subtotal, taxes, discount,
 * advance and balance. Patient acceptance and payment status are recorded against the case.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quote", indexes = {
        @Index(name = "ix_quote_case", columnList = "caseId"),
        @Index(name = "ix_quote_status", columnList = "status")
})
public class Quote extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Column(length = 160)
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @Column(nullable = false, length = 8)
    private String currency = "INR";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_item", joinColumns = @JoinColumn(name = "quote_id"))
    @OrderColumn(name = "line_order")
    private List<QuoteItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(length = 1000)
    private String terms;

    private LocalDate validUntil;

    private Instant acceptedAt;
}
