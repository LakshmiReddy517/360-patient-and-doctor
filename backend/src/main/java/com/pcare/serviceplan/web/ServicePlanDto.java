package com.pcare.serviceplan.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The Case Service Plan (blueprint point 67): every committed service on a case with its resource,
 * status and value, aggregated from all modules into one operational view.
 */
public final class ServicePlanDto {

    private ServicePlanDto() {
    }

    public record PlanItem(String category, String title, String detail, String status, BigDecimal amount) {
    }

    public record Plan(
            Long caseId,
            String caseNumber,
            String patientName,
            List<PlanItem> items,
            int totalItems,
            BigDecimal committedValue) {
    }
}
