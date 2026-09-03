package com.pcare.serviceplan.service;

import com.pcare.billing.service.QuoteService;
import com.pcare.care.service.CareService;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.clinical.service.ClinicalService;
import com.pcare.dispatch.service.AssignmentService;
import com.pcare.fleet.service.FleetService;
import com.pcare.fleet.web.dto.FleetDtos.AmbulanceDto;
import com.pcare.healthcare.service.AppointmentService;
import com.pcare.serviceplan.web.ServicePlanDto.Plan;
import com.pcare.serviceplan.web.ServicePlanDto.PlanItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the Case Service Plan by reading each module's case-scoped services and normalising
 * them into one list. This module depends on the others; none depend on it (no cycles).
 */
@Service
public class ServicePlanService {

    private final CaseService caseService;
    private final QuoteService quoteService;
    private final AssignmentService assignmentService;
    private final FleetService fleetService;
    private final AppointmentService appointmentService;
    private final CareService careService;
    private final ClinicalService clinicalService;

    public ServicePlanService(CaseService caseService, QuoteService quoteService, AssignmentService assignmentService,
                              FleetService fleetService, AppointmentService appointmentService, CareService careService,
                              ClinicalService clinicalService) {
        this.caseService = caseService;
        this.quoteService = quoteService;
        this.assignmentService = assignmentService;
        this.fleetService = fleetService;
        this.appointmentService = appointmentService;
        this.careService = careService;
        this.clinicalService = clinicalService;
    }

    /** Status tokens that mean the service has actually been delivered / fulfilled. */
    private static final java.util.Set<String> DELIVERED_STATES = java.util.Set.of(
            "COMPLETED", "PAID", "DISCHARGED", "ARRIVED", "DELIVERED", "DONE", "CLOSED",
            "FULFILLED", "ACTIVE", "RESOLVED");

    private static boolean isDelivered(String status) {
        return status != null && DELIVERED_STATES.contains(status.toUpperCase());
    }

    /** Builds a plan line, deriving actual (delivered) value from the item's status. */
    private PlanItem item(String category, String title, String detail, String status, BigDecimal planned) {
        boolean delivered = isDelivered(status);
        BigDecimal plan = planned != null ? planned : BigDecimal.ZERO;
        BigDecimal actual = delivered ? plan : BigDecimal.ZERO;
        return new PlanItem(category, title, detail, status, plan, actual, delivered);
    }

    @Transactional(readOnly = true)
    public Plan build(Long caseId) {
        CaseFile c = caseService.get(caseId);
        List<PlanItem> items = new ArrayList<>();

        // Billing quotes
        for (var q : quoteService.listByCase(caseId)) {
            items.add(item("Billing", "Quote #" + q.id(), q.items().size() + " line(s)",
                    q.status().name(), q.total()));
        }
        // Agent dispatch
        for (var a : assignmentService.listByCase(caseId)) {
            items.add(item("Dispatch", "Care agent", a.agentName(), a.status().name(), null));
        }
        // Ambulance
        for (AmbulanceDto amb : fleetService.listAll()) {
            if (caseId.equals(amb.currentCaseId())) {
                items.add(item("Transport", "Ambulance " + amb.category(), amb.registrationNo(),
                        amb.status().name(), null));
            }
        }
        // Appointments
        for (var ap : appointmentService.listByCase(caseId)) {
            items.add(item("Healthcare", "Appointment", ap.doctorName()
                    + (ap.department() != null ? " · " + ap.department() : ""), ap.status().name(), null));
        }
        // Caretaker
        for (var ca : careService.assignmentsByCase(caseId)) {
            items.add(item("Care", "Caretaker", ca.caretakerName(), ca.status().name(), null));
        }
        // Care bookings (accommodation / food / transport)
        for (var b : careService.bookingsByCase(caseId)) {
            items.add(item("Care", b.type().name(), b.provider(), b.status().name(), b.total()));
        }
        // Medicines
        for (var m : clinicalService.medicinesByCase(caseId)) {
            items.add(item("Clinical", "Medicine", m.name()
                    + (m.dose() != null ? " " + m.dose() : ""), m.active() ? "ACTIVE" : "STOPPED", null));
        }

        BigDecimal planned = items.stream().map(PlanItem::plannedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal delivered = items.stream().map(PlanItem::actualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int deliveredCount = (int) items.stream().filter(PlanItem::delivered).count();

        return new Plan(c.getId(), c.getCaseNumber(), c.getPatientName(), items, items.size(),
                deliveredCount, planned, planned, delivered, planned.subtract(delivered));
    }
}
