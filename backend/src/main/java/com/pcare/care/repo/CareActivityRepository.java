package com.pcare.care.repo;

import com.pcare.care.domain.CareActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareActivityRepository extends JpaRepository<CareActivity, Long> {
    List<CareActivity> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
}
