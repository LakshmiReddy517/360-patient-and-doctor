package com.pcare.care.repo;

import com.pcare.care.domain.CaretakerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaretakerAssignmentRepository extends JpaRepository<CaretakerAssignment, Long> {
    List<CaretakerAssignment> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
