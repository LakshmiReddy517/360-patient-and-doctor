package com.pcare.patient.repo;

import com.pcare.patient.domain.MedicalProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalProfileRepository extends JpaRepository<MedicalProfile, Long> {
    Optional<MedicalProfile> findByPatientId(Long patientId);
}
