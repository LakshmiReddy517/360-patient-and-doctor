package com.pcare.inventory;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An allocation of equipment stock to a case, decrementing available stock until returned
 * (blueprint point 27). Returning an allocation replenishes the item's available quantity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipment_allocation", indexes = {
        @Index(name = "ix_allocation_item", columnList = "equipmentItemId"),
        @Index(name = "ix_allocation_case", columnList = "caseId")
})
public class EquipmentAllocation extends BaseEntity {

    @Column(nullable = false)
    private Long equipmentItemId;

    @Column(length = 160)
    private String equipmentName;

    private Long caseId;

    @Column(length = 24)
    private String caseNumber;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false)
    private boolean returned = false;

    private Instant returnedAt;
}
