package com.pcare.patient.repo;

import com.pcare.patient.domain.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentRepository extends JpaRepository<Consent, Long> {
    List<Consent> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
