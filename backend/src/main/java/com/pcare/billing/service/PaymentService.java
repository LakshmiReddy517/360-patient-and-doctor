package com.pcare.billing.service;

import com.pcare.billing.domain.Payment;
import com.pcare.billing.domain.Payment.PaymentType;
import com.pcare.billing.domain.Quote;
import com.pcare.billing.repo.PaymentRepository;
import com.pcare.billing.web.dto.BillingDtos.PaymentDto;
import com.pcare.billing.web.dto.BillingDtos.RecordPaymentRequest;
import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.common.event.NotificationRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Records payments/refunds and keeps the linked quote balance in step (blueprint points 45, 46). */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final QuoteService quoteService;
    private final CaseService caseService;
    private final ApplicationEventPublisher events;

    public PaymentService(PaymentRepository paymentRepository, QuoteService quoteService, CaseService caseService,
                          ApplicationEventPublisher events) {
        this.paymentRepository = paymentRepository;
        this.quoteService = quoteService;
        this.caseService = caseService;
        this.events = events;
    }

    @Transactional
    public Payment record(RecordPaymentRequest req) {
        Payment p = new Payment();
        p.setCaseId(req.caseId());
        p.setQuoteId(req.quoteId());
        p.setAmount(req.amount());
        p.setType(req.type() != null ? req.type() : PaymentType.ADVANCE);
        p.setMethod(req.method() != null ? req.method() : Payment.PaymentMethod.CASH);
        p.setReference(req.reference());
        p.setNote(req.note());
        Payment saved = paymentRepository.save(p);

        if (req.quoteId() != null) {
            Quote q = quoteService.get(req.quoteId());
            BigDecimal signed = saved.getType() == PaymentType.REFUND ? saved.getAmount().negate() : saved.getAmount();
            quoteService.applyPayment(q, signed);
        }

        String verb = saved.getType() == PaymentType.REFUND ? "Refund" : "Payment";
        caseService.addEvent(req.caseId(), CaseEventType.PAYMENT_RECEIVED,
                verb + " " + saved.getAmount() + " via " + saved.getMethod() + " (" + saved.getType() + ")", "BILLING");

        // Event-driven: ask the notification engine to confirm the payment (decoupled).
        CaseFile caseFile = caseService.get(req.caseId());
        events.publishEvent(new NotificationRequestedEvent(
                null, caseFile.getPatientName(), caseFile.getPatientMobile(), "SMS", "PAYMENT",
                verb + " received: " + saved.getAmount(),
                "Your " + verb.toLowerCase() + " of " + saved.getAmount() + " for case " + caseFile.getCaseNumber()
                        + " has been recorded.", req.caseId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> listByCase(Long caseId) {
        return paymentRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toDto).toList();
    }

    private PaymentDto toDto(Payment p) {
        return new PaymentDto(p.getId(), p.getCaseId(), p.getQuoteId(), p.getAmount(), p.getType(),
                p.getMethod(), p.getReference(), p.getNote(), p.getCreatedBy(), p.getCreatedAt());
    }
}
