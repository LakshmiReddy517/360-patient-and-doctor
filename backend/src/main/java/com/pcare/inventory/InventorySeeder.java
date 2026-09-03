package com.pcare.inventory;

import com.pcare.inventory.InventoryDtos.UpsertEquipmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds a starter set of equipment items so the inventory module is demoable on localhost.
 * Idempotent — only seeds when no equipment exists. Disable with app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class InventorySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventorySeeder.class);

    private final EquipmentItemRepository equipmentItemRepository;
    private final InventoryService inventoryService;

    public InventorySeeder(EquipmentItemRepository equipmentItemRepository, InventoryService inventoryService) {
        this.equipmentItemRepository = equipmentItemRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(String... args) {
        if (equipmentItemRepository.count() > 0) {
            return;
        }
        inventoryService.upsertItem(new UpsertEquipmentRequest(
                "Oxygen Cylinder B-type", EquipmentCategory.OXYGEN, "cylinder", 20, true));
        inventoryService.upsertItem(new UpsertEquipmentRequest(
                "Wheelchair", EquipmentCategory.MOBILITY, "unit", 12, true));
        inventoryService.upsertItem(new UpsertEquipmentRequest(
                "Stretcher", EquipmentCategory.MOBILITY, "unit", 8, true));
        inventoryService.upsertItem(new UpsertEquipmentRequest(
                "Patient Monitor", EquipmentCategory.MONITORING, "unit", 6, true));
        inventoryService.upsertItem(new UpsertEquipmentRequest(
                "BiPAP Machine", EquipmentCategory.RESPIRATORY, "unit", 4, true));
        log.info("Seeded {} equipment items", equipmentItemRepository.count());
    }
}
