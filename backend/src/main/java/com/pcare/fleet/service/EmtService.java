package com.pcare.fleet.service;

import com.pcare.common.exception.NotFoundException;
import com.pcare.fleet.domain.EmtDriver;
import com.pcare.fleet.repo.EmtDriverRepository;
import com.pcare.fleet.web.dto.FleetDtos.EmtDto;
import com.pcare.fleet.web.dto.FleetDtos.UpsertEmtRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Driver / EMT registry. */
@Service
public class EmtService {

    private final EmtDriverRepository repository;

    public EmtService(EmtDriverRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EmtDriver create(UpsertEmtRequest req) {
        return repository.save(apply(new EmtDriver(), req));
    }

    @Transactional
    public EmtDriver update(Long id, UpsertEmtRequest req) {
        return repository.save(apply(get(id), req));
    }

    private EmtDriver apply(EmtDriver e, UpsertEmtRequest r) {
        e.setUserId(r.userId());
        e.setFullName(r.fullName());
        e.setMobile(r.mobile());
        e.setLicenceNumber(r.licenceNumber());
        e.setLicenceExpiry(r.licenceExpiry());
        e.setCertification(r.certification());
        e.setCertificationExpiry(r.certificationExpiry());
        e.setMedicalFitnessValid(r.medicalFitnessValid() == null || r.medicalFitnessValid());
        e.setBackgroundVerified(r.backgroundVerified() != null && r.backgroundVerified());
        e.setShift(r.shift());
        return e;
    }

    @Transactional(readOnly = true)
    public EmtDriver get(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("EMT/Driver", id));
    }

    @Transactional
    public EmtDriver setDuty(Long id, boolean onDuty) {
        EmtDriver e = get(id);
        e.setOnDuty(onDuty);
        return repository.save(e);
    }

    @Transactional(readOnly = true)
    public List<EmtDto> listAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public EmtDto toDto(EmtDriver e) {
        return new EmtDto(e.getId(), e.getUserId(), e.getFullName(), e.getMobile(), e.getLicenceNumber(),
                e.getLicenceExpiry(), e.getCertification(), e.getCertificationExpiry(), e.isMedicalFitnessValid(),
                e.isBackgroundVerified(), e.getShift(), e.isOnDuty());
    }
}
