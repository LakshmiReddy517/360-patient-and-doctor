package com.pcare.me;

import com.pcare.billing.domain.Payment;
import com.pcare.billing.service.PaymentService;
import com.pcare.billing.web.dto.BillingDtos.QuoteDto;
import com.pcare.billing.web.dto.BillingDtos.RecordPaymentRequest;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.casefile.web.dto.CaseDtos.CaseSummaryDto;
import com.pcare.casefile.web.dto.CaseDtos.TimelineEventDto;
import com.pcare.clinical.service.ClinicalService;
import com.pcare.clinical.web.dto.ClinicalDtos.MedicineDto;
import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.healthcare.service.AppointmentService;
import com.pcare.healthcare.web.dto.HealthcareDtos.AppointmentDto;
import com.pcare.security.SecurityUtils;
import com.pcare.servicerequest.domain.ServiceRequest;
import com.pcare.servicerequest.repo.ServiceRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Patient-scoped endpoints for the Patient App. Everything is filtered to the logged-in user's own
 * requests/cases (by audit createdBy), so a patient only ever sees their own journey — never others'.
 */
@Tag(name = "Me (Patient App)", description = "The logged-in patient's own cases, appointments, medicines and payments")
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasAnyRole('PATIENT','SUPER_ADMIN','ADMIN')")
public class MeController {

    private final ServiceRequestRepository requestRepository;
    private final CaseService caseService;
    private final com.pcare.billing.service.QuoteService quoteService;
    private final AppointmentService appointmentService;
    private final ClinicalService clinicalService;
    private final PaymentService paymentService;
    private final com.pcare.live.TrackingService trackingService;
    private final com.pcare.patient.repo.PatientRepository patientRepository;

    public MeController(ServiceRequestRepository requestRepository, CaseService caseService,
                        com.pcare.billing.service.QuoteService quoteService, AppointmentService appointmentService,
                        ClinicalService clinicalService, PaymentService paymentService,
                        com.pcare.live.TrackingService trackingService,
                        com.pcare.patient.repo.PatientRepository patientRepository) {
        this.requestRepository = requestRepository;
        this.caseService = caseService;
        this.quoteService = quoteService;
        this.appointmentService = appointmentService;
        this.clinicalService = clinicalService;
        this.paymentService = paymentService;
        this.trackingService = trackingService;
        this.patientRepository = patientRepository;
    }

    @Operation(summary = "My patient master profile (the Patient record linked to my account)")
    @GetMapping("/profile")
    public java.util.Map<String, Object> profile() {
        Long uid = com.pcare.security.SecurityUtils.currentUserId()
                .orElseThrow(() -> new NotFoundException("Not authenticated"));
        var p = patientRepository.findByUserId(uid)
                .orElseThrow(() -> new NotFoundException("No patient profile linked to this account"));
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("patientId", p.getId());
        m.put("fullName", p.getFullName());
        m.put("mobile", p.getMobile());
        m.put("email", p.getEmail());
        return m;
    }

    public record MyCaseDetail(
            CaseSummaryDto caseFile, List<String> services, List<TimelineEventDto> timeline,
            List<QuoteDto> quotes, List<AppointmentDto> appointments, List<MedicineDto> medicines,
            BigDecimal balance) {
    }

    public record PayRequest(@NotNull Long quoteId, @NotNull @Positive BigDecimal amount) {
    }

    private String me() {
        return SecurityUtils.currentUsername().orElseThrow(() -> new NotFoundException("Not authenticated"));
    }

    private List<ServiceRequest> myRequests() {
        return requestRepository.findByCreatedByOrderByCreatedAtDesc(me());
    }

    private Set<Long> myCaseIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (ServiceRequest r : myRequests()) if (r.getCaseId() != null) ids.add(r.getCaseId());
        return ids;
    }

    @Operation(summary = "My cases (from my service requests)")
    @GetMapping("/cases")
    public List<CaseSummaryDto> myCases() {
        List<CaseSummaryDto> out = new ArrayList<>();
        for (Long id : myCaseIds()) {
            try { out.add(caseService.toSummary(caseService.get(id))); } catch (Exception ignored) { }
        }
        return out;
    }

    @Operation(summary = "My requests (including those not yet converted to a case)")
    @GetMapping("/requests")
    public List<Object> myRequestList() {
        List<Object> out = new ArrayList<>();
        for (ServiceRequest r : myRequests()) {
            out.add(java.util.Map.of(
                    "id", r.getId(), "status", r.getStatus().name(),
                    "services", r.getServices().stream().map(Enum::name).toList(),
                    "emergency", r.isEmergency(),
                    "caseNumber", r.getCaseNumber() == null ? "" : r.getCaseNumber()));
        }
        return out;
    }

    @Operation(summary = "Full detail of one of my cases")
    @GetMapping("/cases/{id}")
    public MyCaseDetail myCase(@PathVariable Long id) {
        if (!myCaseIds().contains(id)) {
            throw new NotFoundException("Case not found for this account");
        }
        CaseFile c = caseService.get(id);
        List<String> services = myRequests().stream()
                .filter(r -> id.equals(r.getCaseId())).findFirst()
                .map(r -> r.getServices().stream().map(Enum::name).toList())
                .orElse(List.of());
        List<QuoteDto> quotes = quoteService.listByCase(id);
        BigDecimal balance = quotes.stream().map(QuoteDto::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MyCaseDetail(
                caseService.toSummary(c), services, caseService.getDetail(id).timeline(),
                quotes, appointmentService.listByCase(id), clinicalService.medicinesByCase(id), balance);
    }

    public record MyRecords(List<AppointmentDto> appointments, List<MedicineDto> medicines) {
    }

    @Operation(summary = "My aggregated medical records — appointments & medicines across my cases")
    @GetMapping("/records")
    public MyRecords records() {
        List<AppointmentDto> appts = new ArrayList<>();
        List<MedicineDto> meds = new ArrayList<>();
        for (Long id : myCaseIds()) {
            try {
                appts.addAll(appointmentService.listByCase(id));
                meds.addAll(clinicalService.medicinesByCase(id));
            } catch (Exception ignored) {
            }
        }
        return new MyRecords(appts, meds);
    }

    @Operation(summary = "Live ambulance route for one of my cases (pickup, hospital, driver position)")
    @GetMapping("/cases/{id}/route")
    public com.pcare.live.TrackingDtos.RouteDto caseRoute(@PathVariable Long id) {
        if (!myCaseIds().contains(id)) {
            throw new NotFoundException("Case not found for this account");
        }
        return trackingService.route(id);
    }

    @Operation(summary = "Accept a quote on one of my cases")
    @PostMapping("/quotes/{id}/accept")
    public QuoteDto acceptQuote(@PathVariable Long id) {
        var q = quoteService.get(id);
        if (!myCaseIds().contains(q.getCaseId())) throw new NotFoundException("Quote not found for this account");
        return quoteService.toDto(quoteService.accept(id));
    }

    @Operation(summary = "Pay towards a quote on one of my cases (patient self-service)")
    @PostMapping("/pay")
    public QuoteDto pay(@RequestBody PayRequest req) {
        var q = quoteService.get(req.quoteId());
        if (!myCaseIds().contains(q.getCaseId())) throw new NotFoundException("Quote not found for this account");
        if (req.amount().compareTo(q.getBalance()) > 0) throw new BadRequestException("Amount exceeds balance");
        paymentService.record(new RecordPaymentRequest(q.getCaseId(), req.quoteId(), req.amount(),
                Payment.PaymentType.ADVANCE, Payment.PaymentMethod.UPI, "patient-app", "Paid from Patient App"));
        return quoteService.toDto(quoteService.get(req.quoteId()));
    }
}
