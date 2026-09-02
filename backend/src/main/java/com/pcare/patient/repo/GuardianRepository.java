package com.pcare.patient.repo;

import com.pcare.patient.domain.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    List<Guardian> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
