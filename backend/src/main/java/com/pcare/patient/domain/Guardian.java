package com.pcare.patient.domain;

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

import java.util.HashSet;
import java.util.Set;

/** A guardian/family contact for a patient, with explicit granular permissions. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guardian", indexes = {
        @Index(name = "ix_guardian_patient", columnList = "patientId")
})
public class Guardian extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(length = 60)
    private String relationship;

    @Column(length = 30)
    private String mobile;

    @Column(length = 160)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "guardian_permission", joinColumns = @JoinColumn(name = "guardian_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 40)
    private Set<GuardianPermission> permissions = new HashSet<>();
}
