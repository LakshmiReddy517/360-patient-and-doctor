package com.pcare.healthcare.domain;

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
 * A bed within a hospital (blueprint point 40 — a pragmatic Hospital → Ward/Room → Bed model).
 * Supports admission/accommodation coordination without claiming clinical bed authority unless
 * integrated with the hospital's own system.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bed", indexes = {
        @Index(name = "ix_bed_hospital", columnList = "hospitalId"),
        @Index(name = "ix_bed_status", columnList = "status")
})
public class Bed extends BaseEntity {

    @Column(nullable = false)
    private Long hospitalId;

    @Column(length = 80)
    private String ward;

    @Column(length = 20)
    private String floor;

    @Column(length = 30)
    private String roomNumber;

    @Column(nullable = false, length = 20)
    private String bedNumber;

    /** GENERAL, SEMI_PRIVATE, PRIVATE, ICU, etc. */
    @Column(length = 30)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BedStatus status = BedStatus.AVAILABLE;
}
