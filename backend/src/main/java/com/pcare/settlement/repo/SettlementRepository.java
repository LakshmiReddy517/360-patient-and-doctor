package com.pcare.settlement.repo;

import com.pcare.settlement.domain.Settlement;
import com.pcare.settlement.domain.Settlement.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByOrderByCreatedAtDesc();

    long countByStatus(SettlementStatus status);
}
