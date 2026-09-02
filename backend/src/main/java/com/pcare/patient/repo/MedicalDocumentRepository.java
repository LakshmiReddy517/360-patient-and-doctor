package com.pcare.patient.repo;

import com.pcare.patient.domain.MedicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {
    List<MedicalDocument> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
