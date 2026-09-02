package com.pcare.casefile.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The central Case — one per patient service journey. Everything (service requests, transport,
 * appointments, care, payments, communications, timeline) links back to this Case via its
 * human-readable {@link #caseNumber} such as {@code CASE-2026-000145}.
 *
 * <p>The patient is referenced by id (and a denormalised display name/mobile) so the Case module
 * stays decoupled from the Patient module while still rendering fast lists in the Command Centre.
 * A hard foreign key to the patient table is enforced once the Patient module is present.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "case_file", indexes = {
        @Index(name = "ux_case_number", columnList = "caseNumber", unique = true),
        @Index(name = "ix_case_status", columnList = "status"),
        @Index(name = "ix_case_priority", columnList = "priority"),
        @Index(name = "ix_case_patient", columnList = "patientId")
})
public class CaseFile extends BaseEntity {

    @Column(nullable = false, unique = true, length = 24)
    private String caseNumber;

    private Long patientId;

    @Column(length = 160)
    private String patientName;

    @Column(length = 30)
    private String patientMobile;

    @Column(length = 240)
    private String title;

    @Column(length = 2000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CasePriority priority = CasePriority.NORMAL;

    @Column(nullable = false)
    private boolean emergency = false;

    /** Id of the staff user currently owning coordination of this case, if any. */
    private Long assignedToUserId;

    @Column(length = 160)
    private String assignedToName;
}
