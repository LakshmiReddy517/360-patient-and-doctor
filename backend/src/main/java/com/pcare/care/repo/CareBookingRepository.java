package com.pcare.care.repo;

import com.pcare.care.domain.CareBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareBookingRepository extends JpaRepository<CareBooking, Long> {
    List<CareBooking> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
