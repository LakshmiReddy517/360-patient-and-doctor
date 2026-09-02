package com.pcare.support.repo;

import com.pcare.support.domain.SlaPolicy;
import com.pcare.support.domain.SupportEnums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByPriority(Priority priority);
}
