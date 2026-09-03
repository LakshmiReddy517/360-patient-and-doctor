package com.pcare.casefile.repo;

import com.pcare.casefile.domain.CaseFile;
import com.pcare.casefile.domain.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {

    Optional<CaseFile> findByCaseNumber(String caseNumber);

    Page<CaseFile> findByStatus(CaseStatus status, Pageable pageable);

    Page<CaseFile> findByPatientId(Long patientId, Pageable pageable);

    List<CaseFile> findByEmergencyTrueAndStatusNot(CaseStatus status);

    long countByStatus(CaseStatus status);

    long countByEmergencyTrueAndStatusNot(CaseStatus status);

    /** Cases whose first-response SLA is unmet and already past its target — the live breach count. */
    long countBySlaMetAtIsNullAndSlaTargetAtBefore(java.time.Instant now);

    @Query("select c from CaseFile c where "
            + "(:q is null or lower(c.caseNumber) like lower(concat('%', :q, '%')) "
            + "or lower(c.patientName) like lower(concat('%', :q, '%')) "
            + "or lower(c.title) like lower(concat('%', :q, '%'))) "
            + "and (:status is null or c.status = :status)")
    Page<CaseFile> search(@Param("q") String q, @Param("status") CaseStatus status, Pageable pageable);
}
