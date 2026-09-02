package com.pcare.casefile.service;

import com.pcare.casefile.domain.CaseCounter;
import com.pcare.casefile.repo.CaseCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/** Allocates human-readable case numbers of the form {@code CASE-2026-000145}. */
@Service
public class CaseNumberGenerator {

    private final CaseCounterRepository counterRepository;

    public CaseNumberGenerator(CaseCounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String next() {
        int year = Year.now().getValue();
        CaseCounter counter = counterRepository.findByYear(year)
                .orElseGet(() -> counterRepository.save(new CaseCounter(year)));
        long value = counter.getLastValue() + 1;
        counter.setLastValue(value);
        counterRepository.save(counter);
        return String.format("CASE-%d-%06d", year, value);
    }
}
