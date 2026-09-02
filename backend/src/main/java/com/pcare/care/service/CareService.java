package com.pcare.care.service;

import com.pcare.care.domain.CareActivity;
import com.pcare.care.domain.CareBooking;
import com.pcare.care.domain.CareEnums.CareStatus;
import com.pcare.care.domain.Caretaker;
import com.pcare.care.domain.CaretakerAssignment;
import com.pcare.care.repo.CareActivityRepository;
import com.pcare.care.repo.CareBookingRepository;
import com.pcare.care.repo.CaretakerAssignmentRepository;
import com.pcare.care.repo.CaretakerRepository;
import com.pcare.care.web.dto.CareDtos.*;
import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/** Caretaker workforce, assignments, daily activity diary and unified care bookings. */
@Service
public class CareService {

    private final CaretakerRepository caretakerRepository;
    private final CaretakerAssignmentRepository assignmentRepository;
    private final CareActivityRepository activityRepository;
    private final CareBookingRepository bookingRepository;
    private final CaseService caseService;

    public CareService(CaretakerRepository caretakerRepository, CaretakerAssignmentRepository assignmentRepository,
                       CareActivityRepository activityRepository, CareBookingRepository bookingRepository,
                       CaseService caseService) {
        this.caretakerRepository = caretakerRepository;
        this.assignmentRepository = assignmentRepository;
        this.activityRepository = activityRepository;
        this.bookingRepository = bookingRepository;
        this.caseService = caseService;
    }

    // ---- Caretaker master ----
    @Transactional
    public Caretaker createCaretaker(UpsertCaretakerRequest r) {
        Caretaker c = new Caretaker();
        apply(c, r);
        return caretakerRepository.save(c);
    }

    @Transactional
    public Caretaker updateCaretaker(Long id, UpsertCaretakerRequest r) {
        Caretaker c = getCaretaker(id);
        apply(c, r);
        return caretakerRepository.save(c);
    }

    private void apply(Caretaker c, UpsertCaretakerRequest r) {
        c.setUserId(r.userId());
        c.setFullName(r.fullName());
        c.setMobile(r.mobile());
        c.setSkills(r.skills() != null ? new HashSet<>(r.skills()) : new HashSet<>());
        c.setLanguages(r.languages() != null ? new HashSet<>(r.languages()) : new HashSet<>());
        c.setShift(r.shift());
        c.setHourlyRate(r.hourlyRate() != null ? r.hourlyRate() : BigDecimal.ZERO);
        if (r.verified() != null) c.setVerified(r.verified());
        if (r.available() != null) c.setAvailable(r.available());
    }

    @Transactional(readOnly = true)
    public Caretaker getCaretaker(Long id) {
        return caretakerRepository.findById(id).orElseThrow(() -> NotFoundException.of("Caretaker", id));
    }

    @Transactional(readOnly = true)
    public Page<CaretakerDto> searchCaretakers(String q, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return caretakerRepository.search(query, pageable).map(this::toCaretakerDto);
    }

    @Transactional(readOnly = true)
    public List<CaretakerDto> availableCaretakers() {
        return caretakerRepository.findByAvailableTrue().stream().map(this::toCaretakerDto).toList();
    }

    // ---- Assignment ----
    @Transactional
    public CaretakerAssignment assign(AssignCaretakerRequest r) {
        Caretaker c = getCaretaker(r.caretakerId());
        CaseFile caseFile = caseService.get(r.caseId());
        CaretakerAssignment a = new CaretakerAssignment();
        a.setCaseId(caseFile.getId());
        a.setCaseNumber(caseFile.getCaseNumber());
        a.setPatientName(caseFile.getPatientName());
        a.setCaretakerId(c.getId());
        a.setCaretakerName(c.getFullName());
        a.setStartAt(r.startAt());
        a.setEndAt(r.endAt());
        a.setShift(r.shift());
        a.setHourlyRate(r.hourlyRate() != null ? r.hourlyRate() : c.getHourlyRate());
        a.setResponsibilities(r.responsibilities());
        a.setMealsProvided(r.mealsProvided());
        a.setAccommodationProvided(r.accommodationProvided());
        a.setStatus(CareStatus.CONFIRMED);
        CaretakerAssignment saved = assignmentRepository.save(a);
        caseService.addEvent(caseFile.getId(), CaseEventType.CARE_ACTIVITY,
                "Caretaker " + c.getFullName() + " assigned", "CARE");
        return saved;
    }

    @Transactional
    public CaretakerAssignment setAssignmentStatus(Long id, CareStatus status) {
        CaretakerAssignment a = getAssignment(id);
        a.setStatus(status);
        return assignmentRepository.save(a);
    }

    @Transactional(readOnly = true)
    public CaretakerAssignment getAssignment(Long id) {
        return assignmentRepository.findById(id).orElseThrow(() -> NotFoundException.of("CaretakerAssignment", id));
    }

    @Transactional(readOnly = true)
    public List<CaretakerAssignmentDto> assignmentsByCase(Long caseId) {
        return assignmentRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toAssignmentDto).toList();
    }

    // ---- Activity diary ----
    @Transactional
    public CareActivity addActivity(Long assignmentId, AddActivityRequest r) {
        CaretakerAssignment a = getAssignment(assignmentId);
        CareActivity act = new CareActivity();
        act.setAssignmentId(assignmentId);
        act.setCaseId(a.getCaseId());
        act.setType(r.type());
        act.setNote(r.note());
        CareActivity saved = activityRepository.save(act);
        if (a.getCaseId() != null) {
            caseService.addEvent(a.getCaseId(), CaseEventType.CARE_ACTIVITY,
                    r.type() + ": " + r.note(), "CARE");
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CareActivityDto> activities(Long assignmentId) {
        return activityRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId).stream().map(this::toActivityDto).toList();
    }

    // ---- Unified bookings ----
    @Transactional
    public CareBooking createBooking(CreateCareBookingRequest r) {
        CaseFile caseFile = caseService.get(r.caseId());
        CareBooking b = new CareBooking();
        b.setCaseId(caseFile.getId());
        b.setCaseNumber(caseFile.getCaseNumber());
        b.setPatientName(caseFile.getPatientName());
        b.setType(r.type());
        b.setProvider(r.provider());
        b.setDescription(r.description());
        b.setStartAt(r.startAt());
        b.setEndAt(r.endAt());
        BigDecimal qty = r.quantity() != null ? r.quantity() : BigDecimal.ONE;
        BigDecimal rate = r.unitRate() != null ? r.unitRate() : BigDecimal.ZERO;
        b.setQuantity(qty);
        b.setUnitRate(rate);
        b.setTotal(qty.multiply(rate));
        b.setRoomType(r.roomType());
        b.setDietType(r.dietType());
        b.setMeal(r.meal());
        b.setTripType(r.tripType());
        b.setPickup(r.pickup());
        b.setDestination(r.destination());
        b.setDistanceKm(r.distanceKm());
        b.setStatus(CareStatus.CONFIRMED);
        CareBooking saved = bookingRepository.save(b);
        caseService.addEvent(caseFile.getId(), CaseEventType.CARE_ACTIVITY,
                r.type() + " booked" + (r.provider() != null ? " with " + r.provider() : "")
                        + " — total " + saved.getTotal(), "CARE");
        return saved;
    }

    @Transactional
    public CareBooking setBookingStatus(Long id, CareStatus status) {
        CareBooking b = bookingRepository.findById(id).orElseThrow(() -> NotFoundException.of("CareBooking", id));
        b.setStatus(status);
        return bookingRepository.save(b);
    }

    @Transactional(readOnly = true)
    public List<CareBookingDto> bookingsByCase(Long caseId) {
        return bookingRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toBookingDto).toList();
    }

    // ---- mapping ----
    public CaretakerDto toCaretakerDto(Caretaker c) {
        return new CaretakerDto(c.getId(), c.getUserId(), c.getFullName(), c.getMobile(), c.getSkills(),
                c.getLanguages(), c.getShift(), c.getHourlyRate(), c.isVerified(), c.isAvailable());
    }

    public CaretakerAssignmentDto toAssignmentDto(CaretakerAssignment a) {
        return new CaretakerAssignmentDto(a.getId(), a.getCaseId(), a.getCaseNumber(), a.getPatientName(),
                a.getCaretakerId(), a.getCaretakerName(), a.getStartAt(), a.getEndAt(), a.getShift(),
                a.getHourlyRate(), a.getResponsibilities(), a.isMealsProvided(), a.isAccommodationProvided(),
                a.getStatus(), a.getCreatedAt());
    }

    public CareActivityDto toActivityDto(CareActivity a) {
        return new CareActivityDto(a.getId(), a.getAssignmentId(), a.getCaseId(), a.getType(), a.getNote(),
                a.getCreatedBy(), a.getCreatedAt());
    }

    public CareBookingDto toBookingDto(CareBooking b) {
        return new CareBookingDto(b.getId(), b.getCaseId(), b.getCaseNumber(), b.getPatientName(), b.getType(),
                b.getProvider(), b.getDescription(), b.getStartAt(), b.getEndAt(), b.getQuantity(), b.getUnitRate(),
                b.getTotal(), b.getRoomType(), b.getDietType(), b.getMeal(), b.getTripType(), b.getPickup(),
                b.getDestination(), b.getDistanceKm(), b.getStatus(), b.getCreatedAt());
    }
}
