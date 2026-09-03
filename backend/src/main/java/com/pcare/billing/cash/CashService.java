package com.pcare.billing.cash;

import com.pcare.billing.cash.CashDtos.CashCollectionDto;
import com.pcare.billing.cash.CashDtos.CashInHandSummary;
import com.pcare.billing.cash.CashDtos.CollectCashRequest;
import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Agent cash-in-hand tracking and deposit reconciliation (blueprint point 46). */
@Service
public class CashService {

    private final CashCollectionRepository repo;

    public CashService(CashCollectionRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CashCollectionDto collect(CollectCashRequest req) {
        CashCollection c = new CashCollection();
        c.setAgentId(req.agentId());
        c.setAgentName(req.agentName());
        c.setCaseId(req.caseId());
        c.setCaseNumber(req.caseNumber());
        c.setAmount(req.amount());
        c.setNote(req.note());
        return toDto(repo.save(c));
    }

    /** Marks a single collection deposited, or (when id is null) all of an agent's pending cash. */
    @Transactional
    public List<CashCollectionDto> deposit(Long id, Long agentId, String reference) {
        List<CashCollection> targets;
        if (id != null) {
            CashCollection c = repo.findById(id).orElseThrow(() -> NotFoundException.of("CashCollection", id));
            targets = List.of(c);
        } else if (agentId != null) {
            targets = repo.findByAgentIdAndDepositedOrderByCreatedAtDesc(agentId, false);
        } else {
            throw new BadRequestException("Provide a collection id or an agentId to deposit");
        }
        Instant now = Instant.now();
        for (CashCollection c : targets) {
            if (!c.isDeposited()) {
                c.setDeposited(true);
                c.setDepositedAt(now);
                c.setDepositReference(reference);
                repo.save(c);
            }
        }
        return targets.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CashInHandSummary summary(Long agentId) {
        List<CashCollection> all = repo.findByAgentIdOrderByCreatedAtDesc(agentId);
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal deposited = BigDecimal.ZERO;
        int pending = 0;
        for (CashCollection c : all) {
            collected = collected.add(c.getAmount());
            if (c.isDeposited()) {
                deposited = deposited.add(c.getAmount());
            } else {
                pending++;
            }
        }
        return new CashInHandSummary(agentId, collected, deposited, collected.subtract(deposited),
                all.size(), pending);
    }

    @Transactional(readOnly = true)
    public List<CashCollectionDto> listByAgent(Long agentId) {
        return repo.findByAgentIdOrderByCreatedAtDesc(agentId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CashCollectionDto> listPendingDeposits() {
        return repo.findByDepositedOrderByCreatedAtDesc(false).stream().map(this::toDto).toList();
    }

    private CashCollectionDto toDto(CashCollection c) {
        return new CashCollectionDto(c.getId(), c.getAgentId(), c.getAgentName(), c.getCaseId(), c.getCaseNumber(),
                c.getAmount(), c.getNote(), c.isDeposited(), c.getDepositedAt(), c.getDepositReference(),
                c.getCreatedAt());
    }
}
