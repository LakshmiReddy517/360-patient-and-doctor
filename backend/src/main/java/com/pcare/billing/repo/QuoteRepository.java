package com.pcare.billing.repo;

import com.pcare.billing.domain.Quote;
import com.pcare.billing.domain.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    long countByStatus(QuoteStatus status);
}
