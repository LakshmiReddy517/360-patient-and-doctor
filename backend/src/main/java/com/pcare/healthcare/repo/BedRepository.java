package com.pcare.healthcare.repo;

import com.pcare.healthcare.domain.Bed;
import com.pcare.healthcare.domain.BedStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BedRepository extends JpaRepository<Bed, Long> {
    List<Bed> findByHospitalIdOrderByWardAscBedNumberAsc(Long hospitalId);

    long countByHospitalIdAndStatus(Long hospitalId, BedStatus status);
}
