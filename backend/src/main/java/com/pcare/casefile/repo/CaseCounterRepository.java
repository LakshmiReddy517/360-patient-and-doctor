package com.pcare.casefile.repo;

import com.pcare.casefile.domain.CaseCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CaseCounterRepository extends JpaRepository<CaseCounter, Integer> {

    /** Pessimistic lock so concurrent case creation never allocates duplicate numbers. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CaseCounter> findByYear(Integer year);
}
