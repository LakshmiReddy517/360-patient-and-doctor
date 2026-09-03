package com.pcare.dispatch.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * A care agent / field workforce member (blueprint point 19). Holds identity, skills, languages,
 * verification, availability status, current location and workload used by the dispatch engine.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "agent", indexes = {
        @Index(name = "ix_agent_status", columnList = "status"),
        @Index(name = "ix_agent_user", columnList = "userId")
})
public class Agent extends BaseEntity {

    /** Optional link to the login account used by the Care Agent app. */
    private Long userId;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(length = 30)
    private String mobile;

    @Column(length = 160)
    private String email;

    @Column(length = 40)
    private String employeeCode;

    @Column(length = 40)
    private String identityType;

    @Column(length = 60)
    private String identityNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_skill", joinColumns = @JoinColumn(name = "agent_id"))
    @Column(name = "skill", length = 60)
    private Set<String> skills = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_language", joinColumns = @JoinColumn(name = "agent_id"))
    @Column(name = "language", length = 40)
    private Set<String> languages = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus status = AgentStatus.OFFLINE;

    private Double currentLatitude;

    private Double currentLongitude;

    private Instant locationUpdatedAt;

    @Column(length = 40)
    private String shift;

    @Column(nullable = false)
    private double rating = 5.0;

    /** Number of currently active assignments — used for workload-aware dispatch. */
    @Column(nullable = false)
    private int activeAssignments = 0;

    @Column(nullable = false)
    private boolean verified = false;

    private LocalDate documentExpiry;

    // ---- EMT / paramedic qualification (blueprint point 28) ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EmtLevel emtLevel = EmtLevel.NONE;

    @Column(length = 8)
    private String bloodGroup;

    @Column(length = 60)
    private String licenceNumber;

    private LocalDate certificationExpiry;
}
