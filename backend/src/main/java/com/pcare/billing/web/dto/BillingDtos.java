package com.pcare.billing.web.dto;

import com.pcare.billing.domain.Payment.PaymentMethod;
import com.pcare.billing.domain.Payment.PaymentType;
import com.pcare.billing.domain.PackageCategory;
import com.pcare.billing.domain.QuoteStatus;
import com.pcare.servicerequest.domain.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** DTOs for the billing module (rate cards, packages, quotes, payments). */
public final class BillingDtos {

    private BillingDtos() {
    }

    // ---- Rate cards ----
    public record RateCardDto(Long id, ServiceType serviceType, String label, String unit,
                              BigDecimal unitRate, BigDecimal baseFare, BigDecimal perKmRate,
                              BigDecimal emergencyMultiplier, BigDecimal nightSurchargePercent, boolean active) {
    }

    public record UpsertRateCardRequest(
            @NotNull ServiceType serviceType, @NotBlank String label, @NotBlank String unit,
            @NotNull BigDecimal unitRate, BigDecimal baseFare, BigDecimal perKmRate,
            BigDecimal emergencyMultiplier, BigDecimal nightSurchargePercent, Boolean active) {
    }

    /** Multi-factor fare estimate breakdown (blueprint point 17). */
    public record FareEstimate(
            ServiceType serviceType, String label, double distanceKm, boolean emergency, boolean night,
            BigDecimal baseFare, BigDecimal distanceCharge, BigDecimal subtotal,
            BigDecimal emergencySurcharge, BigDecimal nightSurcharge, BigDecimal total, String currency) {
    }

    // ---- Packages ----
    public record PackageItemDto(ServiceType serviceType, String label, BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record PackageDto(Long id, String name, String description, PackageCategory category,
                             boolean active, List<PackageItemDto> items, BigDecimal indicativeTotal) {
    }

    public record UpsertPackageRequest(
            @NotBlank String name, String description, PackageCategory category,
            Boolean active, List<PackageItemDto> items) {
    }

    // ---- Quotes ----
    public record QuoteLineRequest(ServiceType serviceType, String label,
                                   @NotNull @Positive BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record CreateQuoteRequest(
            @NotNull Long caseId,
            Long packageId,
            List<QuoteLineRequest> lines,
            BigDecimal taxPercent,
            BigDecimal discountAmount,
            BigDecimal advanceAmount,
            @Size(max = 1000) String terms,
            LocalDate validUntil) {
    }

    public record QuoteItemDto(ServiceType serviceType, String label, BigDecimal quantity,
                               BigDecimal unitPrice, BigDecimal amount) {
    }

    public record QuoteDto(
            Long id, Long caseId, String caseNumber, String patientName, QuoteStatus status, String currency,
            List<QuoteItemDto> items, BigDecimal subtotal, BigDecimal taxPercent, BigDecimal taxAmount,
            BigDecimal discountAmount, BigDecimal total, BigDecimal advanceAmount, BigDecimal amountPaid,
            BigDecimal balance, String terms, LocalDate validUntil, Instant acceptedAt, Instant createdAt) {
    }

    // ---- Payments ----
    public record RecordPaymentRequest(
            @NotNull Long caseId, Long quoteId, @NotNull @Positive BigDecimal amount,
            PaymentType type, PaymentMethod method, String reference, @Size(max = 300) String note) {
    }

    public record PaymentDto(Long id, Long caseId, Long quoteId, BigDecimal amount, PaymentType type,
                             PaymentMethod method, String reference, String note, String recordedBy, Instant createdAt) {
    }
}
