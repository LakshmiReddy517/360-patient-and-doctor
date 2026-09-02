package com.pcare.intelligence;

import com.pcare.billing.service.QuoteService;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.serviceplan.service.ServicePlanService;
import com.pcare.serviceplan.web.ServicePlanDto.Plan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operational intelligence (blueprint points 55, 56, 74). Rule-based service recommendations and a
 * heuristic case summary. Per the AI guardrails (point 80), this never diagnoses, prescribes or
 * gives clinical advice — it only suggests operational services and summarises coordination.
 */
@Tag(name = "Intelligence", description = "Service recommendations and operational case summary (guardrailed)")
@RestController
@RequestMapping("/api/v1")
public class IntelligenceController {

    private final CaseService caseService;
    private final ServicePlanService servicePlanService;
    private final QuoteService quoteService;

    public IntelligenceController(CaseService caseService, ServicePlanService servicePlanService,
                                  QuoteService quoteService) {
        this.caseService = caseService;
        this.servicePlanService = servicePlanService;
        this.quoteService = quoteService;
    }

    public record RecommendRequest(
            boolean travellingAlone, boolean airportOrRailwayArrival, boolean needsAdmission,
            boolean hasDoctorAppointment, boolean needsAccommodation, boolean mobilityAssistance,
            boolean extendedStay, String notes) {
    }

    public record Recommendation(String service, String reason) {
    }

    public record RecommendResponse(List<Recommendation> recommendations, String disclaimer) {
    }

    @Operation(summary = "Recommend operational services from stated needs (not medical advice)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT','PATIENT')")
    @PostMapping("/recommendations")
    public RecommendResponse recommend(@Valid @RequestBody RecommendRequest r) {
        Map<String, String> rec = new LinkedHashMap<>();
        if (r.airportOrRailwayArrival() || r.travellingAlone()) {
            rec.put("PICKUP_DROP", "Patient is arriving" + (r.travellingAlone() ? " alone" : "")
                    + " and needs assisted pickup/drop");
        }
        if (r.mobilityAssistance()) {
            rec.put("AMBULANCE", "Mobility assistance / stretcher transport indicated");
        }
        if (r.needsAdmission()) {
            rec.put("HOSPITAL_ADMISSION", "Hospital admission assistance required");
        }
        if (r.hasDoctorAppointment()) {
            rec.put("DOCTOR_APPOINTMENT", "A doctor consultation is planned");
        }
        if (r.needsAccommodation() || (r.travellingAlone() && r.extendedStay())) {
            rec.put("ACCOMMODATION", "Accommodation near the hospital is advisable");
        }
        if (r.extendedStay()) {
            rec.put("CARETAKER", "Extended stay benefits from a dedicated caretaker");
            rec.put("FOOD", "Meal coordination for the stay");
        }
        rec.put("LOCAL_TRANSPORT", "Local transport for hospital visits and errands");

        List<Recommendation> list = new ArrayList<>();
        rec.forEach((s, why) -> list.add(new Recommendation(s, why)));
        return new RecommendResponse(list,
                "Operational recommendation based on stated needs only. This does not diagnose disease "
                        + "or provide medical advice; clinical decisions remain with qualified professionals.");
    }

    public record CaseSummary(Long caseId, String caseNumber, String summary, String disclaimer) {
    }

    @Operation(summary = "Generate a heuristic operational summary of a case (guardrailed)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @GetMapping("/cases/{id}/ai-summary")
    public CaseSummary summary(@PathVariable Long id) {
        CaseFile c = caseService.get(id);
        Plan plan = servicePlanService.build(id);
        var timeline = caseService.getDetail(id).timeline();

        long emergencies = timeline.stream().filter(e -> "EMERGENCY_ESCALATED".equals(e.type().name())).count();
        BigDecimal paid = quoteService.listByCase(id).stream()
                .map(q -> q.amountPaid()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = quoteService.listByCase(id).stream()
                .map(q -> q.balance()).reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append(c.getCaseNumber()).append(" for ").append(c.getPatientName())
                .append(" is currently ").append(c.getStatus()).append(". ");
        sb.append(timeline.size()).append(" coordinated events recorded");
        if (emergencies > 0) sb.append(", including ").append(emergencies).append(" emergency escalation(s)");
        sb.append(". ");
        if (!plan.items().isEmpty()) {
            var areas = plan.items().stream().map(i -> i.category()).distinct().toList();
            sb.append(plan.totalItems()).append(" services committed across ")
                    .append(String.join(", ", areas)).append(" (value ₹").append(plan.committedValue()).append("). ");
        }
        sb.append("Financials: ₹").append(paid).append(" paid, ₹").append(balance).append(" balance. ");
        sb.append(c.getStatus().name().equals("CLOSED")
                ? "The case has been completed and closed."
                : "Coordination is in progress.");

        return new CaseSummary(c.getId(), c.getCaseNumber(), sb.toString(),
                "Generated operational summary. Not a clinical record; diagnosis, prescribing and medical "
                        + "counselling remain with authorised healthcare professionals.");
    }
}
