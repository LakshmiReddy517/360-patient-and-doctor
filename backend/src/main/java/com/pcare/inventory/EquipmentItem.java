package com.pcare.inventory;

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
 * A type of medical equipment held in inventory with running stock levels (blueprint point 27).
 * {@code availableQuantity} is the quantity not currently allocated to a case.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipment_item", indexes = {
        @Index(name = "ix_equipment_category", columnList = "category")
})
public class EquipmentItem extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EquipmentCategory category = EquipmentCategory.OTHER;

    @Column(length = 40)
    private String unit;

    @Column(nullable = false)
    private int totalQuantity = 0;

    @Column(nullable = false)
    private int availableQuantity = 0;

    @Column(nullable = false)
    private boolean active = true;
}
