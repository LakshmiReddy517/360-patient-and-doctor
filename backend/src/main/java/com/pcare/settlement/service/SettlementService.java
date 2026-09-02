package com.pcare.settlement.service;

import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.settlement.domain.Earning;
import com.pcare.settlement.domain.Settlement;
import com.pcare.settlement.domain.Settlement.SettlementStatus;
import com.pcare.settlement.repo.EarningRepository;
import com.pcare.settlement.repo.SettlementRepository;
import com.pcare.settlement.web.dto.SettlementDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Earnings accrual, settlement batches and payout reconciliation. */
@Service
public class SettlementService {

    private final EarningRepository earningRepository;
    private final SettlementRepository settlementRepository;

    public SettlementService(EarningRepository earningRepository, SettlementRepository settlementRepository) {
        this.earningRepository = earningRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public Earning accrue(AccrueEarningRequest r) {
        Earning e = new Earning();
        e.setPayeeType(r.payeeType());
        e.setPayeeId(r.payeeId());
        e.setPayeeName(r.payeeName());
        e.setCaseId(r.caseId());
        e.setCaseNumber(r.caseNumber());
        e.setDescription(r.description());
        e.setAmount(r.amount());
        return earningRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<EarningDto> unsettled() {
        return earningRepository.findBySettledFalse().stream().map(this::toEarningDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EarningDto> earningsOfSettlement(Long settlementId) {
        return earningRepository.findBySettlementId(settlementId).stream().map(this::toEarningDto).toList();
    }

    /** Create a settlement from all unsettled earnings for a payee. */
    @Transactional
    public Settlement createSettlement(CreateSettlementRequest r) {
        List<Earning> earnings = earningRepository.findByPayeeTypeAndPayeeIdAndSettledFalse(r.payeeType(), r.payeeId());
        if (earnings.isEmpty()) {
            throw new BadRequestException("No unsettled earnings for this payee");
        }
        BigDecimal gross = earnings.stream().map(Earning::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deductions = r.deductions() != null ? r.deductions() : BigDecimal.ZERO;

        Settlement s = new Settlement();
        s.setPayeeType(r.payeeType());
        s.setPayeeId(r.payeeId());
        s.setPayeeName(r.payeeName() != null ? r.payeeName() : earnings.get(0).getPayeeName());
        s.setPeriodLabel(r.periodLabel());
        s.setGrossAmount(gross);
        s.setDeductions(deductions);
        s.setNetAmount(gross.subtract(deductions).max(BigDecimal.ZERO));
        s.setLineCount(earnings.size());
        s.setStatus(SettlementStatus.PENDING);
        Settlement saved = settlementRepository.save(s);

        earnings.forEach(e -> {
            e.setSettled(true);
            e.setSettlementId(saved.getId());
        });
        earningRepository.saveAll(earnings);
        return saved;
    }

    @Transactional
    public Settlement updateStatus(Long id, SettlementStatus status, String reference) {
        Settlement s = settlementRepository.findById(id).orElseThrow(() -> NotFoundException.of("Settlement", id));
        if (status == SettlementStatus.CANCELLED) {
            // release earnings back to unsettled
            earningRepository.findBySettlementId(id).forEach(e -> {
                e.setSettled(false);
                e.setSettlementId(null);
                earningRepository.save(e);
            });
        }
        if (status == SettlementStatus.APPROVED) s.setApprovedAt(Instant.now());
        if (status == SettlementStatus.PAID) {
            s.setPaidAt(Instant.now());
            s.setPaymentReference(reference);
        }
        s.setStatus(status);
        return settlementRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<SettlementDto> list() {
        return settlementRepository.findByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SettlementStats stats() {
        BigDecimal unsettledTotal = earningRepository.findBySettledFalse().stream()
                .map(Earning::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SettlementStats(
                settlementRepository.countByStatus(SettlementStatus.PENDING),
                settlementRepository.countByStatus(SettlementStatus.APPROVED),
                settlementRepository.countByStatus(SettlementStatus.PAID),
                unsettledTotal);
    }

    public SettlementDto toDto(Settlement s) {
        return new SettlementDto(s.getId(), s.getPayeeType(), s.getPayeeId(), s.getPayeeName(), s.getPeriodLabel(),
                s.getGrossAmount(), s.getDeductions(), s.getNetAmount(), s.getLineCount(), s.getStatus(),
                s.getPaymentReference(), s.getApprovedAt(), s.getPaidAt(), s.getCreatedAt());
    }

    public EarningDto toEarningDto(Earning e) {
        return new EarningDto(e.getId(), e.getPayeeType(), e.getPayeeId(), e.getPayeeName(), e.getCaseId(),
                e.getCaseNumber(), e.getDescription(), e.getAmount(), e.isSettled(), e.getSettlementId(), e.getCreatedAt());
    }
}
