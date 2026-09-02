package com.pcare.clinical.repo;

import com.pcare.clinical.domain.MedicationIntake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationIntakeRepository extends JpaRepository<MedicationIntake, Long> {
    List<MedicationIntake> findByMedicineIdOrderByCreatedAtDesc(Long medicineId);

    List<MedicationIntake> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
