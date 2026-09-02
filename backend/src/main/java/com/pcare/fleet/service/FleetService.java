package com.pcare.fleet.service;

import com.pcare.casefile.domain.CaseEventType;
import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.service.CaseService;
import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.fleet.domain.Ambulance;
import com.pcare.fleet.domain.AmbulanceCategory;
import com.pcare.fleet.domain.AmbulanceStatus;
import com.pcare.fleet.domain.ReadinessCheck;
import com.pcare.fleet.repo.AmbulanceRepository;
import com.pcare.fleet.repo.ReadinessCheckRepository;
import com.pcare.fleet.web.dto.FleetDtos.AmbulanceDto;
import com.pcare.fleet.web.dto.FleetDtos.FleetStats;
import com.pcare.fleet.web.dto.FleetDtos.ReadinessRequest;
import com.pcare.fleet.web.dto.FleetDtos.ReadinessResult;
import com.pcare.fleet.web.dto.FleetDtos.UpsertAmbulanceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Ambulance fleet: registration, readiness (category-aware), dispatch gating and live tracking. */
@Service
public class FleetService {

    /** Minimum oxygen level required to dispatch a BLS/ALS ambulance. */
    private static final int MIN_OXYGEN_FOR_CLINICAL = 20;

    private final AmbulanceRepository ambulanceRepository;
    private final ReadinessCheckRepository readinessRepository;
    private final CaseService caseService;
    private final com.pcare.live.LiveHub liveHub;

    public FleetService(AmbulanceRepository ambulanceRepository, ReadinessCheckRepository readinessRepository,
                        CaseService caseService, com.pcare.live.LiveHub liveHub) {
        this.ambulanceRepository = ambulanceRepository;
        this.readinessRepository = readinessRepository;
        this.caseService = caseService;
        this.liveHub = liveHub;
    }

    @Transactional
    public Ambulance create(UpsertAmbulanceRequest req) {
        Ambulance a = new Ambulance();
        apply(a, req);
        return ambulanceRepository.save(a);
    }

    @Transactional
    public Ambulance update(Long id, UpsertAmbulanceRequest req) {
        Ambulance a = get(id);
        apply(a, req);
        return ambulanceRepository.save(a);
    }

    private void apply(Ambulance a, UpsertAmbulanceRequest r) {
        a.setCode(r.code());
        a.setRegistrationNo(r.registrationNo());
        a.setCategory(r.category());
        a.setOwnerName(r.ownerName());
        a.setInsuranceExpiry(r.insuranceExpiry());
        a.setFitnessExpiry(r.fitnessExpiry());
        a.setPermitExpiry(r.permitExpiry());
        a.setDriverId(r.driverId());
        a.setDriverName(r.driverName());
    }

    @Transactional(readOnly = true)
    public Ambulance get(Long id) {
        return ambulanceRepository.findById(id).orElseThrow(() -> NotFoundException.of("Ambulance", id));
    }

    @Transactional(readOnly = true)
    public List<AmbulanceDto> listAll() {
        return ambulanceRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AmbulanceDto> listActive() {
        return ambulanceRepository.findByStatusNot(AmbulanceStatus.OFFLINE).stream().map(this::toDto).toList();
    }

    @Transactional
    public Ambulance setStatus(Long id, AmbulanceStatus status) {
        Ambulance a = get(id);
        a.setStatus(status);
        if (status == AmbulanceStatus.AVAILABLE || status == AmbulanceStatus.OFFLINE) {
            a.setCurrentCaseId(null);
            a.setCurrentCaseNumber(null);
            a.setCurrentPatientName(null);
        }
        return ambulanceRepository.save(a);
    }

    @Transactional
    public Ambulance updateLocation(Long id, double lat, double lng) {
        Ambulance a = get(id);
        a.setCurrentLatitude(lat);
        a.setCurrentLongitude(lng);
        a.setLocationUpdatedAt(Instant.now());
        Ambulance saved = ambulanceRepository.save(a);
        liveHub.broadcast("location", java.util.Map.of(
                "kind", "ambulance", "id", saved.getId(), "name", saved.getRegistrationNo(),
                "lat", lat, "lng", lng, "status", saved.getStatus().name()));
        return saved;
    }

    /** Submit a readiness checklist; applies category-specific required items and updates the vehicle. */
    @Transactional
    public ReadinessResult submitReadiness(Long ambulanceId, ReadinessRequest r) {
        Ambulance a = get(ambulanceId);
        boolean base = r.fuelOk() && r.engineOk() && r.tyresOk() && r.batteryOk() && r.lightsOk() && r.gpsOk();
        boolean clinical = r.sirenOk() && r.stretcherOk() && r.oxygenOk() && r.firstAidPpeOk()
                && r.reportedOxygenPercent() >= MIN_OXYGEN_FOR_CLINICAL;
        boolean passed = switch (a.getCategory()) {
            case PATIENT_TRANSPORT -> base;
            case BLS -> base && clinical;
            case ALS -> base && clinical && r.suctionOk();
        };

        ReadinessCheck check = new ReadinessCheck();
        check.setAmbulanceId(ambulanceId);
        check.setFuelOk(r.fuelOk()); check.setEngineOk(r.engineOk()); check.setTyresOk(r.tyresOk());
        check.setBatteryOk(r.batteryOk()); check.setLightsOk(r.lightsOk()); check.setSirenOk(r.sirenOk());
        check.setGpsOk(r.gpsOk()); check.setStretcherOk(r.stretcherOk()); check.setWheelchairOk(r.wheelchairOk());
        check.setOxygenOk(r.oxygenOk()); check.setSuctionOk(r.suctionOk()); check.setFirstAidPpeOk(r.firstAidPpeOk());
        check.setReportedOxygenPercent(r.reportedOxygenPercent());
        check.setReportedFuelPercent(r.reportedFuelPercent());
        check.setNotes(r.notes());
        check.setPassed(passed);
        ReadinessCheck saved = readinessRepository.save(check);

        a.setReady(passed);
        a.setOxygenLevelPercent(r.reportedOxygenPercent());
        a.setFuelPercent(r.reportedFuelPercent());
        a.setLastCheckAt(Instant.now());
        if (passed && (a.getStatus() == AmbulanceStatus.OFFLINE || a.getStatus() == AmbulanceStatus.MAINTENANCE)) {
            a.setStatus(AmbulanceStatus.AVAILABLE);
        }
        if (!passed && a.getStatus() == AmbulanceStatus.AVAILABLE) {
            a.setStatus(AmbulanceStatus.MAINTENANCE);
        }
        ambulanceRepository.save(a);

        String message = passed ? "Ambulance is ready for dispatch"
                : "Readiness FAILED — required items missing for " + a.getCategory();
        return new ReadinessResult(saved.getId(), ambulanceId, passed, message,
                saved.getReportedOxygenPercent(), saved.getReportedFuelPercent(), saved.getCreatedAt());
    }

    @Transactional
    public Ambulance assign(Long ambulanceId, Long caseId, String note) {
        Ambulance a = get(ambulanceId);
        if (!a.isReady()) {
            throw new BadRequestException("Ambulance " + a.getRegistrationNo() + " has not passed a readiness check");
        }
        if (!a.getStatus().isDispatchable()) {
            throw new BadRequestException("Ambulance is not available (status " + a.getStatus() + ")");
        }
        if (a.getCategory() != AmbulanceCategory.PATIENT_TRANSPORT && a.getOxygenLevelPercent() < MIN_OXYGEN_FOR_CLINICAL) {
            throw new BadRequestException("Oxygen level too low (" + a.getOxygenLevelPercent() + "%) to dispatch a "
                    + a.getCategory() + " ambulance");
        }
        CaseFile caseFile = caseService.get(caseId);
        a.setCurrentCaseId(caseFile.getId());
        a.setCurrentCaseNumber(caseFile.getCaseNumber());
        a.setCurrentPatientName(caseFile.getPatientName());
        a.setStatus(AmbulanceStatus.EN_ROUTE);
        ambulanceRepository.save(a);

        caseService.addEvent(caseId, CaseEventType.RESOURCE_ASSIGNED,
                "Ambulance " + a.getRegistrationNo() + " (" + a.getCategory() + ") dispatched"
                        + (note != null ? " — " + note : ""), "FLEET");
        return a;
    }

    @Transactional(readOnly = true)
    public FleetStats stats() {
        long total = ambulanceRepository.count();
        long available = ambulanceRepository.countByStatus(AmbulanceStatus.AVAILABLE);
        long maintenance = ambulanceRepository.countByStatus(AmbulanceStatus.MAINTENANCE);
        long onTrip = ambulanceRepository.countByStatus(AmbulanceStatus.EN_ROUTE)
                + ambulanceRepository.countByStatus(AmbulanceStatus.AT_PICKUP)
                + ambulanceRepository.countByStatus(AmbulanceStatus.TRANSPORTING)
                + ambulanceRepository.countByStatus(AmbulanceStatus.AT_HOSPITAL);
        return new FleetStats(total, available, onTrip, maintenance);
    }

    public AmbulanceDto toDto(Ambulance a) {
        return new AmbulanceDto(a.getId(), a.getCode(), a.getRegistrationNo(), a.getCategory(), a.getOwnerName(),
                a.getInsuranceExpiry(), a.getFitnessExpiry(), a.getPermitExpiry(), a.getDriverId(), a.getDriverName(),
                a.getStatus(), a.getCurrentLatitude(), a.getCurrentLongitude(), a.getLocationUpdatedAt(),
                a.getOxygenLevelPercent(), a.getFuelPercent(), a.isReady(), a.getLastCheckAt(),
                a.getCurrentCaseId(), a.getCurrentCaseNumber(), a.getCurrentPatientName());
    }
}
