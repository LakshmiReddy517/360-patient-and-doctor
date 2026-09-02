package com.pcare.care.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * A caretaker — a separate workforce from short-duration pickup agents (blueprint point 35).
 * Longer-duration patient care with its own verification, skills and availability.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "caretaker", indexes = {
        @Index(name = "ix_caretaker_user", columnList = "userId")
})
public class Caretaker extends BaseEntity {

    private Long userId;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(length = 30)
    private String mobile;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "caretaker_skill", joinColumns = @JoinColumn(name = "caretaker_id"))
    @Column(name = "skill", length = 60)
    private Set<String> skills = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "caretaker_language", joinColumns = @JoinColumn(name = "caretaker_id"))
    @Column(name = "language", length = 40)
    private Set<String> languages = new HashSet<>();

    @Column(length = 30)
    private String shift;

    @Column(precision = 10, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean available = true;
}
