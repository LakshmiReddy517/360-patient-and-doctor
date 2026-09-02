package com.pcare.support.repo;

import com.pcare.support.domain.Complaint;
import com.pcare.support.domain.SupportEnums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    @Query("select c from Complaint c where "
            + "(:q is null or lower(c.subject) like lower(concat('%', :q, '%')) or lower(c.patientName) like lower(concat('%', :q, '%'))) "
            + "and (:status is null or c.status = :status) order by c.createdAt desc")
    Page<Complaint> search(@Param("q") String q, @Param("status") ComplaintStatus status, Pageable pageable);

    long countByStatus(ComplaintStatus status);

    @Query("select count(c) from Complaint c where c.resolvedAt is null and c.slaResolutionDueAt < :now "
            + "and c.status <> com.pcare.support.domain.SupportEnums$ComplaintStatus.CLOSED")
    long countResolutionBreached(@Param("now") Instant now);
}
