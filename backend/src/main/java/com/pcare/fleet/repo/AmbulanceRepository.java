package com.pcare.fleet.repo;

import com.pcare.fleet.domain.Ambulance;
import com.pcare.fleet.domain.AmbulanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByStatus(AmbulanceStatus status);

    List<Ambulance> findByStatusNot(AmbulanceStatus status);

    long countByStatus(AmbulanceStatus status);
}
