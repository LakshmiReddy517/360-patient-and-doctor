package com.pcare.billing.cash;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/** DTOs for agent cash collection & reconciliation (blueprint point 46). */
public final class CashDtos {

    private CashDtos() {
    }

    public record CollectCashRequest(
            @NotNull Long agentId, String agentName, Long caseId, String caseNumber,
            @NotNull @Positive BigDecimal amount, String note) {
    }

    public record DepositRequest(@NotNull Long agentId, String reference) {
    }

    public record CashCollectionDto(
            Long id, Long agentId, String agentName, Long caseId, String caseNumber, BigDecimal amount,
            String note, boolean deposited, Instant depositedAt, String depositReference, Instant createdAt) {
    }

    public record CashInHandSummary(
            Long agentId, BigDecimal collected, BigDecimal deposited, BigDecimal inHand,
            int totalCollections, int pendingDeposits) {
    }
}
