package com.pcare.healthcare.service;

import com.pcare.common.exception.NotFoundException;
import com.pcare.healthcare.domain.Bed;
import com.pcare.healthcare.domain.BedStatus;
import com.pcare.healthcare.domain.Hospital;
import com.pcare.healthcare.repo.BedRepository;
import com.pcare.healthcare.repo.HospitalRepository;
import com.pcare.healthcare.web.dto.HealthcareDtos.BedDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.HospitalDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertBedRequest;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertHospitalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/** Hospital master and bed/room management. */
@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final BedRepository bedRepository;

    public HospitalService(HospitalRepository hospitalRepository, BedRepository bedRepository) {
        this.hospitalRepository = hospitalRepository;
        this.bedRepository = bedRepository;
    }

    @Transactional
    public Hospital create(UpsertHospitalRequest req) {
        return hospitalRepository.save(apply(new Hospital(), req));
    }

    @Transactional
    public Hospital update(Long id, UpsertHospitalRequest req) {
        return hospitalRepository.save(apply(get(id), req));
    }

    private Hospital apply(Hospital h, UpsertHospitalRequest r) {
        h.setName(r.name());
        h.setAddressLine(r.addressLine());
        h.setCity(r.city());
        h.setState(r.state());
        h.setPhone(r.phone());
        h.setEmail(r.email());
        h.setEmergencyAvailable(r.emergencyAvailable());
        h.setOpdTiming(r.opdTiming());
        h.setPartner(r.partner());
        h.setActive(r.active() == null || r.active());
        h.setDepartments(r.departments() != null ? new HashSet<>(r.departments()) : new HashSet<>());
        h.setServices(r.services() != null ? new HashSet<>(r.services()) : new HashSet<>());
        return h;
    }

    @Transactional(readOnly = true)
    public Hospital get(Long id) {
        return hospitalRepository.findById(id).orElseThrow(() -> NotFoundException.of("Hospital", id));
    }

    @Transactional(readOnly = true)
    public Page<HospitalDto> search(String q, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return hospitalRepository.search(query, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public HospitalDto getDto(Long id) {
        return toDto(get(id));
    }

    // ---- Beds ----
    @Transactional
    public Bed addBed(UpsertBedRequest req) {
        get(req.hospitalId());
        Bed b = new Bed();
        b.setHospitalId(req.hospitalId());
        b.setWard(req.ward());
        b.setFloor(req.floor());
        b.setRoomNumber(req.roomNumber());
        b.setBedNumber(req.bedNumber());
        b.setType(req.type());
        b.setStatus(req.status() != null ? req.status() : BedStatus.AVAILABLE);
        return bedRepository.save(b);
    }

    @Transactional
    public Bed setBedStatus(Long bedId, BedStatus status) {
        Bed b = bedRepository.findById(bedId).orElseThrow(() -> NotFoundException.of("Bed", bedId));
        b.setStatus(status);
        return bedRepository.save(b);
    }

    @Transactional(readOnly = true)
    public List<BedDto> listBeds(Long hospitalId) {
        return bedRepository.findByHospitalIdOrderByWardAscBedNumberAsc(hospitalId).stream().map(this::toBedDto).toList();
    }

    // ---- mapping ----
    public HospitalDto toDto(Hospital h) {
        long total = h.getId() != null ? bedRepository.findByHospitalIdOrderByWardAscBedNumberAsc(h.getId()).size() : 0;
        long available = h.getId() != null ? bedRepository.countByHospitalIdAndStatus(h.getId(), BedStatus.AVAILABLE) : 0;
        return new HospitalDto(h.getId(), h.getName(), h.getAddressLine(), h.getCity(), h.getState(),
                h.getPhone(), h.getEmail(), h.isEmergencyAvailable(), h.getOpdTiming(), h.isPartner(),
                h.isActive(), h.getDepartments(), h.getServices(), total, available);
    }

    public BedDto toBedDto(Bed b) {
        return new BedDto(b.getId(), b.getHospitalId(), b.getWard(), b.getFloor(), b.getRoomNumber(),
                b.getBedNumber(), b.getType(), b.getStatus());
    }
}
