package com.pcare.patient.repo;

import com.pcare.patient.domain.DocumentAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLog, Long> {

    List<DocumentAccessLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    List<DocumentAccessLog> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
