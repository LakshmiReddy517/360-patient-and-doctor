package com.pcare.support.domain;

import com.pcare.common.domain.BaseEntity;
import com.pcare.support.domain.SupportEnums.Priority;
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
 * Configurable SLA targets per priority (blueprint point 68). Response and resolution windows in
 * minutes drive the due-date and breach computation on complaints.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sla_policy", indexes = {
        @Index(name = "ux_sla_priority", columnList = "priority", unique = true)
})
public class SlaPolicy extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Column(nullable = false)
    private int responseMinutes;

    @Column(nullable = false)
    private int resolutionMinutes;
}
