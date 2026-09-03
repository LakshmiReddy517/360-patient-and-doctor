package com.pcare.serviceplan.web;

import java.math.BigDecimal;
import java.util.List;

/**
 * The Case Service Plan (blueprint point 67): every committed service on a case with its resource,
 * status and value, aggregated from all modules — now with a planned-vs-actual delivery comparison.
 */
public final class ServicePlanDto {

    private ServicePlanDto() {
    }

    /**
     * One line of the plan. {@code plannedAmount} is the value committed; {@code actualAmount} is the
     * value actually delivered so far (equals planned once {@code delivered} is true, else zero/null).
     */
    public record PlanItem(String category, String title, String detail, String status,
                           BigDecimal plannedAmount, BigDecimal actualAmount, boolean delivered) {
    }

    public record Plan(
            Long caseId,
            String caseNumber,
            String patientName,
            List<PlanItem> items,
            int totalItems,
            int deliveredItems,
            BigDecimal committedValue,
            BigDecimal plannedValue,
            BigDecimal deliveredValue,
            BigDecimal varianceValue) {
    }
}
