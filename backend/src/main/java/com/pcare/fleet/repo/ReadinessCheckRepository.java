package com.pcare.fleet.repo;

import com.pcare.fleet.domain.ReadinessCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReadinessCheckRepository extends JpaRepository<ReadinessCheck, Long> {
    List<ReadinessCheck> findByAmbulanceIdOrderByCreatedAtDesc(Long ambulanceId);
}
