package com.pcare.healthcare.service;

import com.pcare.common.exception.NotFoundException;
import com.pcare.healthcare.domain.Doctor;
import com.pcare.healthcare.repo.DoctorRepository;
import com.pcare.healthcare.web.dto.HealthcareDtos.DoctorDto;
import com.pcare.healthcare.web.dto.HealthcareDtos.UpsertDoctorRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Doctor registry. */
@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public Doctor create(UpsertDoctorRequest req) {
        return doctorRepository.save(apply(new Doctor(), req));
    }

    @Transactional
    public Doctor update(Long id, UpsertDoctorRequest req) {
        return doctorRepository.save(apply(get(id), req));
    }

    private Doctor apply(Doctor d, UpsertDoctorRequest r) {
        d.setUserId(r.userId());
        d.setFullName(r.fullName());
        d.setSpecialty(r.specialty());
        d.setDepartment(r.department());
        d.setHospitalId(r.hospitalId());
        d.setHospitalName(r.hospitalName());
        d.setQualification(r.qualification());
        d.setRegistrationNumber(r.registrationNumber());
        d.setPhone(r.phone());
        d.setEmail(r.email());
        d.setConsultationFee(r.consultationFee() != null ? r.consultationFee() : BigDecimal.ZERO);
        d.setAvailable(r.available() == null || r.available());
        return d;
    }

    @Transactional(readOnly = true)
    public Doctor get(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> NotFoundException.of("Doctor", id));
    }

    @Transactional(readOnly = true)
    public Page<DoctorDto> search(String q, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return doctorRepository.search(query, pageable).map(this::toDto);
    }

    public DoctorDto toDto(Doctor d) {
        return new DoctorDto(d.getId(), d.getUserId(), d.getFullName(), d.getSpecialty(), d.getDepartment(),
                d.getHospitalId(), d.getHospitalName(), d.getQualification(), d.getRegistrationNumber(),
                d.getPhone(), d.getEmail(), d.getConsultationFee(), d.isAvailable());
    }
}
