package com.pcare.clinical.repo;

import com.pcare.clinical.domain.PatientCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientConditionRepository extends JpaRepository<PatientCondition, Long> {
    List<PatientCondition> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    List<PatientCondition> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
