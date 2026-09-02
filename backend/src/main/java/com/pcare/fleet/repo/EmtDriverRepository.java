package com.pcare.fleet.repo;

import com.pcare.fleet.domain.EmtDriver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmtDriverRepository extends JpaRepository<EmtDriver, Long> {
    Optional<EmtDriver> findByUserId(Long userId);
}
