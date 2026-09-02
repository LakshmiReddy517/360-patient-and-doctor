package com.pcare.settlement.web.dto;

import com.pcare.settlement.domain.Settlement.PayeeType;
import com.pcare.settlement.domain.Settlement.SettlementStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/** DTOs for the finance settlements module. */
public final class SettlementDtos {

    private SettlementDtos() {
    }

    public record AccrueEarningRequest(
            @NotNull PayeeType payeeType, @NotNull Long payeeId, String payeeName, Long caseId,
            String caseNumber, String description, @NotNull @Positive BigDecimal amount) {
    }

    public record EarningDto(
            Long id, PayeeType payeeType, Long payeeId, String payeeName, Long caseId, String caseNumber,
            String description, BigDecimal amount, boolean settled, Long settlementId, Instant createdAt) {
    }

    public record CreateSettlementRequest(
            @NotNull PayeeType payeeType, @NotNull Long payeeId, String payeeName, String periodLabel,
            BigDecimal deductions) {
    }

    public record SettlementStatusRequest(@NotNull SettlementStatus status, String paymentReference) {
    }

    public record SettlementDto(
            Long id, PayeeType payeeType, Long payeeId, String payeeName, String periodLabel,
            BigDecimal grossAmount, BigDecimal deductions, BigDecimal netAmount, int lineCount,
            SettlementStatus status, String paymentReference, Instant approvedAt, Instant paidAt, Instant createdAt) {
    }

    public record SettlementStats(long pending, long approved, long paid, BigDecimal unsettledTotal) {
    }
}
