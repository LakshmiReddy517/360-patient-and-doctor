package com.pcare.inventory;

import com.pcare.inventory.InventoryDtos.AllocateRequest;
import com.pcare.inventory.InventoryDtos.AllocationDto;
import com.pcare.inventory.InventoryDtos.EquipmentItemDto;
import com.pcare.inventory.InventoryDtos.UpsertEquipmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Inventory", description = "Medical equipment inventory and case allocation (blueprint point 27)")
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "List all equipment items with stock levels")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','AGENT')")
    @GetMapping("/equipment")
    public List<EquipmentItemDto> listItems() {
        return inventoryService.listItems();
    }

    @Operation(summary = "Create or update an equipment item (matched by name)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/equipment")
    public EquipmentItemDto upsertItem(@Valid @RequestBody UpsertEquipmentRequest req) {
        return inventoryService.upsertItem(req);
    }

    @Operation(summary = "Allocate equipment stock to a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PostMapping("/allocate")
    public AllocationDto allocate(@Valid @RequestBody AllocateRequest req) {
        return inventoryService.allocate(req);
    }

    @Operation(summary = "Return an allocation, replenishing available stock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @PostMapping("/allocations/{id}/return")
    public AllocationDto returnAllocation(@PathVariable Long id) {
        return inventoryService.returnAllocation(id);
    }

    @Operation(summary = "List equipment allocations for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AGENT')")
    @GetMapping("/cases/{caseId}/allocations")
    public List<AllocationDto> allocationsByCase(@PathVariable Long caseId) {
        return inventoryService.allocationsByCase(caseId);
    }
}
