package com.pcare.billing.cash;

import com.pcare.billing.cash.CashDtos.CashCollectionDto;
import com.pcare.billing.cash.CashDtos.CashInHandSummary;
import com.pcare.billing.cash.CashDtos.CollectCashRequest;
import com.pcare.billing.cash.CashDtos.DepositRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Cash", description = "Agent cash collection and deposit reconciliation (blueprint point 46)")
@RestController
@RequestMapping("/api/v1/cash")
public class CashController {

    private final CashService cashService;

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    @Operation(summary = "Record cash collected from a patient on a trip (agent app)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','AGENT')")
    @PostMapping("/collect")
    public CashCollectionDto collect(@Valid @RequestBody CollectCashRequest req) {
        return cashService.collect(req);
    }

    @Operation(summary = "Deposit a single cash collection (mark reconciled)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @PostMapping("/{id}/deposit")
    public List<CashCollectionDto> deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest req) {
        return cashService.deposit(id, req.agentId(), req.reference());
    }

    @Operation(summary = "Deposit all of an agent's pending cash in one settlement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @PostMapping("/deposit")
    public List<CashCollectionDto> depositAll(@Valid @RequestBody DepositRequest req) {
        return cashService.deposit(null, req.agentId(), req.reference());
    }

    @Operation(summary = "An agent's cash-in-hand summary (collected vs deposited)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','AGENT')")
    @GetMapping("/agent/{agentId}/summary")
    public CashInHandSummary summary(@PathVariable Long agentId) {
        return cashService.summary(agentId);
    }

    @Operation(summary = "An agent's cash collections")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','AGENT')")
    @GetMapping("/agent/{agentId}")
    public List<CashCollectionDto> listByAgent(@PathVariable Long agentId) {
        return cashService.listByAgent(agentId);
    }

    @Operation(summary = "All cash pending deposit across agents (finance reconciliation)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @GetMapping("/pending")
    public List<CashCollectionDto> pending() {
        return cashService.listPendingDeposits();
    }
}
