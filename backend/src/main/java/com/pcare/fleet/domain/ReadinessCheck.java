package com.pcare.fleet.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A pre-duty readiness checklist submission for an ambulance (blueprint point 26). Which items are
 * required varies by vehicle category — that rule is applied in the service, not hard-coded in a screen.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "readiness_check", indexes = {
        @Index(name = "ix_readiness_ambulance", columnList = "ambulanceId")
})
public class ReadinessCheck extends BaseEntity {

    @Column(nullable = false)
    private Long ambulanceId;

    private boolean fuelOk;
    private boolean engineOk;
    private boolean tyresOk;
    private boolean batteryOk;
    private boolean lightsOk;
    private boolean sirenOk;
    private boolean gpsOk;
    private boolean stretcherOk;
    private boolean wheelchairOk;
    private boolean oxygenOk;
    private boolean suctionOk;
    private boolean firstAidPpeOk;

    private int reportedOxygenPercent = 100;
    private int reportedFuelPercent = 100;

    @Column(length = 500)
    private String notes;

    /** Computed: whether the required items for the ambulance's category all passed. */
    @Column(nullable = false)
    private boolean passed;
}
