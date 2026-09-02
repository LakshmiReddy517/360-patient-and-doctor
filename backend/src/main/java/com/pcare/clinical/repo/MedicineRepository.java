package com.pcare.clinical.repo;

import com.pcare.clinical.domain.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    List<Medicine> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
