package com.pcare.casefile.repo;

import com.pcare.casefile.domain.CaseTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseTimelineEventRepository extends JpaRepository<CaseTimelineEvent, Long> {
    List<CaseTimelineEvent> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
