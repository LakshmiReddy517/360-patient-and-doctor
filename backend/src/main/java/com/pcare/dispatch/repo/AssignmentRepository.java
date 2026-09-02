package com.pcare.dispatch.repo;

import com.pcare.dispatch.domain.Assignment;
import com.pcare.dispatch.domain.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    List<Assignment> findByAgentIdOrderByCreatedAtDesc(Long agentId);

    List<Assignment> findByAgentIdAndStatusNotIn(Long agentId, List<AssignmentStatus> statuses);

    List<Assignment> findByStatusNotInOrderByCreatedAtDesc(List<AssignmentStatus> statuses);
}
