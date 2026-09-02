package com.pcare.dispatch.repo;

import com.pcare.dispatch.domain.Agent;
import com.pcare.dispatch.domain.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByUserId(Long userId);

    List<Agent> findByStatusIn(List<AgentStatus> statuses);

    long countByStatus(AgentStatus status);
}
