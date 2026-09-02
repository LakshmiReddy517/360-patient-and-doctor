package com.pcare.billing.repo;

import com.pcare.billing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    List<Payment> findByQuoteIdOrderByCreatedAtDesc(Long quoteId);
}
