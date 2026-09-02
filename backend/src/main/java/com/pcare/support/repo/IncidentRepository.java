package com.pcare.support.repo;

import com.pcare.support.domain.Incident;
import com.pcare.support.domain.SupportEnums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("select i from Incident i where (:status is null or i.status = :status) order by i.createdAt desc")
    Page<Incident> search(@Param("status") IncidentStatus status, Pageable pageable);

    long countByStatus(IncidentStatus status);
}
