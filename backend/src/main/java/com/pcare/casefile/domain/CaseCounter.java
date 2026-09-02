package com.pcare.casefile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Per-year monotonic counter backing human-readable case numbers (CASE-YYYY-NNNNNN). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "case_counter")
public class CaseCounter {

    @Id
    @Column(name = "year_value")
    private Integer year;

    @Column(name = "last_number", nullable = false)
    private Long lastValue = 0L;

    public CaseCounter(Integer year) {
        this.year = year;
        this.lastValue = 0L;
    }
}
