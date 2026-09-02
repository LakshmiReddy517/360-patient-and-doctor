package com.pcare.billing.web;

import com.pcare.billing.service.PaymentService;
import com.pcare.billing.service.QuoteService;
import com.pcare.billing.web.dto.BillingDtos.CreateQuoteRequest;
import com.pcare.billing.web.dto.BillingDtos.PaymentDto;
import com.pcare.billing.web.dto.BillingDtos.QuoteDto;
import com.pcare.billing.web.dto.BillingDtos.RecordPaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Billing", description = "Quotes, acceptance and payments")
@RestController
@RequestMapping("/api/v1")
public class BillingController {

    private final QuoteService quoteService;
    private final PaymentService paymentService;

    public BillingController(QuoteService quoteService, PaymentService paymentService) {
        this.quoteService = quoteService;
        this.paymentService = paymentService;
    }

    // ---- Quotes ----
    @Operation(summary = "Create a quote for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @PostMapping("/quotes")
    public QuoteDto create(@Valid @RequestBody CreateQuoteRequest req) {
        return quoteService.toDto(quoteService.create(req));
    }

    @Operation(summary = "List quotes for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','PATIENT')")
    @GetMapping("/quotes")
    public List<QuoteDto> listByCase(@RequestParam Long caseId) {
        return quoteService.listByCase(caseId);
    }

    @Operation(summary = "Get a quote")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','PATIENT')")
    @GetMapping("/quotes/{id}")
    public QuoteDto get(@PathVariable Long id) {
        return quoteService.toDto(quoteService.get(id));
    }

    @Operation(summary = "Send a quote to the patient")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @PostMapping("/quotes/{id}/send")
    public QuoteDto send(@PathVariable Long id) {
        return quoteService.toDto(quoteService.send(id));
    }

    @Operation(summary = "Accept a quote (patient or admin on behalf)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','PATIENT')")
    @PostMapping("/quotes/{id}/accept")
    public QuoteDto accept(@PathVariable Long id) {
        return quoteService.toDto(quoteService.accept(id));
    }

    @Operation(summary = "Reject a quote")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','PATIENT')")
    @PostMapping("/quotes/{id}/reject")
    public QuoteDto reject(@PathVariable Long id) {
        return quoteService.toDto(quoteService.reject(id));
    }

    // ---- Payments ----
    @Operation(summary = "Record a payment or refund")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','AGENT')")
    @PostMapping("/payments")
    public PaymentDto record(@Valid @RequestBody RecordPaymentRequest req) {
        return toDto(paymentService.record(req));
    }

    @Operation(summary = "List payments for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @GetMapping("/payments")
    public List<PaymentDto> listPaymentsByCase(@RequestParam Long caseId) {
        return paymentService.listByCase(caseId);
    }

    private PaymentDto toDto(com.pcare.billing.domain.Payment p) {
        return new PaymentDto(p.getId(), p.getCaseId(), p.getQuoteId(), p.getAmount(), p.getType(),
                p.getMethod(), p.getReference(), p.getNote(), p.getCreatedBy(), p.getCreatedAt());
    }
}
