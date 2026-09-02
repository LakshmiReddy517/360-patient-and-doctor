package com.pcare.settlement.web;

import com.pcare.settlement.service.SettlementService;
import com.pcare.settlement.web.dto.SettlementDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Settlements", description = "Agent/partner earnings, settlement batches and payout reconciliation")
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Operation(summary = "Settlement stats")
    @GetMapping("/settlements/stats")
    public SettlementStats stats() {
        return settlementService.stats();
    }

    @Operation(summary = "Accrue an earning for an agent/partner")
    @PostMapping("/earnings")
    public EarningDto accrue(@Valid @RequestBody AccrueEarningRequest req) {
        return settlementService.toEarningDto(settlementService.accrue(req));
    }

    @Operation(summary = "List unsettled earnings")
    @GetMapping("/earnings/unsettled")
    public List<EarningDto> unsettled() {
        return settlementService.unsettled();
    }

    @Operation(summary = "List settlements")
    @GetMapping("/settlements")
    public List<SettlementDto> list() {
        return settlementService.list();
    }

    @Operation(summary = "Earnings included in a settlement")
    @GetMapping("/settlements/{id}/earnings")
    public List<EarningDto> earnings(@PathVariable Long id) {
        return settlementService.earningsOfSettlement(id);
    }

    @Operation(summary = "Create a settlement from a payee's unsettled earnings")
    @PostMapping("/settlements")
    public SettlementDto create(@Valid @RequestBody CreateSettlementRequest req) {
        return settlementService.toDto(settlementService.createSettlement(req));
    }

    @Operation(summary = "Approve / pay / cancel a settlement")
    @PutMapping("/settlements/{id}/status")
    public SettlementDto updateStatus(@PathVariable Long id, @Valid @RequestBody SettlementStatusRequest req) {
        return settlementService.toDto(settlementService.updateStatus(id, req.status(), req.paymentReference()));
    }
}
