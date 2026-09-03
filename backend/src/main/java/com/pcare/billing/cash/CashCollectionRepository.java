package com.pcare.billing.cash;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashCollectionRepository extends JpaRepository<CashCollection, Long> {

    List<CashCollection> findByAgentIdOrderByCreatedAtDesc(Long agentId);

    List<CashCollection> findByAgentIdAndDepositedOrderByCreatedAtDesc(Long agentId, boolean deposited);

    List<CashCollection> findByDepositedOrderByCreatedAtDesc(boolean deposited);
}
