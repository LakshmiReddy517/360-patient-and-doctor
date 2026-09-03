package com.pcare.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/** DTOs for the equipment inventory module (blueprint point 27). */
public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record UpsertEquipmentRequest(
            @NotBlank String name, EquipmentCategory category, String unit,
            @NotNull Integer totalQuantity, Boolean active) {
    }

    public record EquipmentItemDto(
            Long id, String name, EquipmentCategory category, String unit,
            int totalQuantity, int availableQuantity, int allocatedQuantity, boolean active) {
    }

    public record AllocateRequest(
            @NotNull Long equipmentItemId, Long caseId, String caseNumber,
            @NotNull @Positive Integer quantity) {
    }

    public record AllocationDto(
            Long id, Long equipmentItemId, String equipmentName, Long caseId, String caseNumber,
            int quantity, boolean returned, Instant returnedAt, Instant createdAt) {
    }
}
