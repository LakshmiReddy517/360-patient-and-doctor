package com.pcare.billing.service;

import com.pcare.billing.domain.PackageItem;
import com.pcare.billing.domain.Quote;
import com.pcare.billing.domain.QuoteItem;
import com.pcare.billing.domain.QuoteStatus;
import com.pcare.billing.domain.RateCard;
import com.pcare.billing.domain.ServicePackage;
import com.pcare.billing.repo.QuoteRepository;
import com.pcare.billing.repo.RateCardRepository;
import com.pcare.billing.web.dto.BillingDtos.CreateQuoteRequest;
import com.pcare.billing.web.dto.BillingDtos.QuoteDto;
import com.pcare.billing.web.dto.BillingDtos.QuoteItemDto;
import com.pcare.billing.web.dto.BillingDtos.QuoteLineRequest;
import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Pricing engine + quote lifecycle (blueprint points 17, 18). */
@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final RateCardRepository rateCardRepository;
    private final CatalogService catalogService;
    private final CaseService caseService;

    public QuoteService(QuoteRepository quoteRepository, RateCardRepository rateCardRepository,
                        CatalogService catalogService, CaseService caseService) {
        this.quoteRepository = quoteRepository;
        this.rateCardRepository = rateCardRepository;
        this.catalogService = catalogService;
        this.caseService = caseService;
    }

    @Transactional
    public Quote create(CreateQuoteRequest req) {
        CaseFile caseFile = caseService.get(req.caseId());
        Quote q = new Quote();
        q.setCaseId(caseFile.getId());
        q.setCaseNumber(caseFile.getCaseNumber());
        q.setPatientName(caseFile.getPatientName());
        q.setTaxPercent(nz(req.taxPercent()));
        q.setDiscountAmount(nz(req.discountAmount()));
        q.setAdvanceAmount(nz(req.advanceAmount()));
        q.setTerms(req.terms());
        q.setValidUntil(req.validUntil());

        List<QuoteItem> items = new ArrayList<>();
        if (req.packageId() != null) {
            ServicePackage pkg = catalogService.getPackage(req.packageId());
            for (PackageItem pi : pkg.getItems()) {
                items.add(line(pi.getServiceType(), pi.getLabel(), pi.getQuantity(), pi.getUnitPrice()));
            }
        }
        if (req.lines() != null) {
            for (QuoteLineRequest l : req.lines()) {
                items.add(line(l.serviceType(), l.label(), l.quantity(), l.unitPrice()));
            }
        }
        if (items.isEmpty()) {
            throw new BadRequestException("A quote needs at least one line (package or explicit lines)");
        }
        q.setItems(items);
        recalc(q);

        Quote saved = quoteRepository.save(q);
        caseService.addEvent(caseFile.getId(), CaseEventType.QUOTE_PREPARED,
                "Quote #" + saved.getId() + " prepared — total " + saved.getCurrency() + " " + saved.getTotal(), "BILLING");
        return saved;
    }

    /** Builds a line; when no unit price is supplied it is taken from the service's rate card. */
    private QuoteItem line(com.pcare.servicerequest.domain.ServiceType type, String label,
                           BigDecimal quantity, BigDecimal unitPriceOverride) {
        BigDecimal qty = quantity != null ? quantity : BigDecimal.ONE;
        BigDecimal unitPrice = unitPriceOverride;
        String finalLabel = label;
        if (unitPrice == null && type != null) {
            RateCard rc = rateCardRepository.findByServiceType(type).orElse(null);
            if (rc != null) {
                unitPrice = rc.getUnitRate();
                if (finalLabel == null || finalLabel.isBlank()) finalLabel = rc.getLabel();
            }
        }
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        if (finalLabel == null || finalLabel.isBlank()) finalLabel = type != null ? type.name() : "Service";

        QuoteItem qi = new QuoteItem();
        qi.setServiceType(type);
        qi.setLabel(finalLabel);
        qi.setQuantity(qty);
        qi.setUnitPrice(unitPrice);
        qi.setAmount(unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP));
        return qi;
    }

    private void recalc(Quote q) {
        BigDecimal subtotal = q.getItems().stream().map(QuoteItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(q.getTaxPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).subtract(q.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
        if (total.signum() < 0) total = BigDecimal.ZERO;
        q.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        q.setTaxAmount(tax);
        q.setTotal(total);
        q.setBalance(total.subtract(q.getAmountPaid()).setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public Quote get(Long id) {
        return quoteRepository.findById(id).orElseThrow(() -> NotFoundException.of("Quote", id));
    }

    @Transactional(readOnly = true)
    public List<QuoteDto> listByCase(Long caseId) {
        return quoteRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toDto).toList();
    }

    @Transactional
    public Quote send(Long id) {
        Quote q = get(id);
        if (q.getStatus() != QuoteStatus.DRAFT && q.getStatus() != QuoteStatus.SENT) {
            throw new BadRequestException("Only draft quotes can be sent");
        }
        q.setStatus(QuoteStatus.SENT);
        quoteRepository.save(q);
        caseService.addEvent(q.getCaseId(), CaseEventType.QUOTE_SENT, "Quote #" + q.getId() + " sent to patient", "BILLING");
        return q;
    }

    @Transactional
    public Quote accept(Long id) {
        Quote q = get(id);
        if (q.getStatus() == QuoteStatus.REJECTED || q.getStatus() == QuoteStatus.EXPIRED) {
            throw new BadRequestException("Cannot accept a " + q.getStatus() + " quote");
        }
        q.setStatus(QuoteStatus.ACCEPTED);
        q.setAcceptedAt(Instant.now());
        quoteRepository.save(q);
        caseService.addEvent(q.getCaseId(), CaseEventType.QUOTE_ACCEPTED,
                "Quote #" + q.getId() + " accepted — balance due " + q.getCurrency() + " " + q.getBalance(), "BILLING");
        return q;
    }

    @Transactional
    public Quote reject(Long id) {
        Quote q = get(id);
        q.setStatus(QuoteStatus.REJECTED);
        quoteRepository.save(q);
        caseService.addEvent(q.getCaseId(), CaseEventType.NOTE_ADDED, "Quote #" + q.getId() + " rejected", "BILLING");
        return q;
    }

    /** Applies a payment to the quote and refreshes paid/balance/status. Called by PaymentService. */
    @Transactional
    public void applyPayment(Quote q, BigDecimal signedAmount) {
        q.setAmountPaid(q.getAmountPaid().add(signedAmount).setScale(2, RoundingMode.HALF_UP));
        if (q.getAmountPaid().signum() < 0) q.setAmountPaid(BigDecimal.ZERO);
        q.setBalance(q.getTotal().subtract(q.getAmountPaid()).setScale(2, RoundingMode.HALF_UP));
        if (q.getBalance().signum() <= 0 && q.getStatus() == QuoteStatus.ACCEPTED) {
            q.setStatus(QuoteStatus.PAID);
        }
        quoteRepository.save(q);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public QuoteDto toDto(Quote q) {
        List<QuoteItemDto> items = q.getItems().stream()
                .map(i -> new QuoteItemDto(i.getServiceType(), i.getLabel(), i.getQuantity(), i.getUnitPrice(), i.getAmount()))
                .toList();
        return new QuoteDto(q.getId(), q.getCaseId(), q.getCaseNumber(), q.getPatientName(), q.getStatus(),
                q.getCurrency(), items, q.getSubtotal(), q.getTaxPercent(), q.getTaxAmount(), q.getDiscountAmount(),
                q.getTotal(), q.getAdvanceAmount(), q.getAmountPaid(), q.getBalance(), q.getTerms(),
                q.getValidUntil(), q.getAcceptedAt(), q.getCreatedAt());
    }
}
