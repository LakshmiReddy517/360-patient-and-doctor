package com.pcare.servicerequest.repo;

import com.pcare.servicerequest.domain.RequestStatus;
import com.pcare.servicerequest.domain.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    Page<ServiceRequest> findByPatientId(Long patientId, Pageable pageable);

    java.util.Optional<ServiceRequest> findFirstByCaseId(Long caseId);

    java.util.List<ServiceRequest> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    long countByStatus(RequestStatus status);

    @Query("select r from ServiceRequest r where "
            + "(:q is null or lower(r.patientName) like lower(concat('%', :q, '%')) "
            + "or r.patientMobile like concat('%', :q, '%')) "
            + "and (:status is null or r.status = :status)")
    Page<ServiceRequest> search(@Param("q") String q, @Param("status") RequestStatus status, Pageable pageable);
}
