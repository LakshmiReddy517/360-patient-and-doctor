package com.pcare.inventory;

import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.inventory.InventoryDtos.AllocateRequest;
import com.pcare.inventory.InventoryDtos.AllocationDto;
import com.pcare.inventory.InventoryDtos.EquipmentItemDto;
import com.pcare.inventory.InventoryDtos.UpsertEquipmentRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Equipment stock keeping and case allocation/return tracking (blueprint point 27). */
@Service
public class InventoryService {

    private final EquipmentItemRepository itemRepo;
    private final EquipmentAllocationRepository allocationRepo;

    public InventoryService(EquipmentItemRepository itemRepo, EquipmentAllocationRepository allocationRepo) {
        this.itemRepo = itemRepo;
        this.allocationRepo = allocationRepo;
    }

    /** Creates a new item, or updates the existing one with a matching (case-insensitive) name. */
    @Transactional
    public EquipmentItemDto upsertItem(UpsertEquipmentRequest req) {
        EquipmentItem item = itemRepo.findFirstByNameIgnoreCase(req.name()).orElse(null);
        int newTotal = req.totalQuantity();
        if (item == null) {
            item = new EquipmentItem();
            item.setName(req.name());
            item.setTotalQuantity(newTotal);
            item.setAvailableQuantity(newTotal);
        } else {
            int delta = newTotal - item.getTotalQuantity();
            item.setTotalQuantity(newTotal);
            item.setAvailableQuantity(Math.max(0, item.getAvailableQuantity() + delta));
        }
        item.setCategory(req.category() != null ? req.category() : EquipmentCategory.OTHER);
        item.setUnit(req.unit());
        if (req.active() != null) {
            item.setActive(req.active());
        }
        return toDto(itemRepo.save(item));
    }

    @Transactional(readOnly = true)
    public List<EquipmentItemDto> listItems() {
        return itemRepo.findAll(Sort.by(Sort.Direction.ASC, "name")).stream().map(this::toDto).toList();
    }

    @Transactional
    public AllocationDto allocate(AllocateRequest req) {
        EquipmentItem item = itemRepo.findById(req.equipmentItemId())
                .orElseThrow(() -> NotFoundException.of("EquipmentItem", req.equipmentItemId()));
        int qty = req.quantity();
        if (item.getAvailableQuantity() < qty) {
            throw new BadRequestException("Not enough " + item.getName()
                    + " in stock: available " + item.getAvailableQuantity());
        }
        item.setAvailableQuantity(item.getAvailableQuantity() - qty);
        itemRepo.save(item);

        EquipmentAllocation alloc = new EquipmentAllocation();
        alloc.setEquipmentItemId(item.getId());
        alloc.setEquipmentName(item.getName());
        alloc.setCaseId(req.caseId());
        alloc.setCaseNumber(req.caseNumber());
        alloc.setQuantity(qty);
        return toDto(allocationRepo.save(alloc));
    }

    @Transactional
    public AllocationDto returnAllocation(Long allocationId) {
        EquipmentAllocation alloc = allocationRepo.findById(allocationId)
                .orElseThrow(() -> NotFoundException.of("EquipmentAllocation", allocationId));
        if (alloc.isReturned()) {
            throw new BadRequestException("Allocation already returned: " + allocationId);
        }
        alloc.setReturned(true);
        alloc.setReturnedAt(Instant.now());
        allocationRepo.save(alloc);

        itemRepo.findById(alloc.getEquipmentItemId()).ifPresent(item -> {
            item.setAvailableQuantity(Math.min(item.getTotalQuantity(),
                    item.getAvailableQuantity() + alloc.getQuantity()));
            itemRepo.save(item);
        });
        return toDto(alloc);
    }

    @Transactional(readOnly = true)
    public List<AllocationDto> allocationsByCase(Long caseId) {
        return allocationRepo.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toDto).toList();
    }

    private EquipmentItemDto toDto(EquipmentItem i) {
        return new EquipmentItemDto(i.getId(), i.getName(), i.getCategory(), i.getUnit(),
                i.getTotalQuantity(), i.getAvailableQuantity(),
                i.getTotalQuantity() - i.getAvailableQuantity(), i.isActive());
    }

    private AllocationDto toDto(EquipmentAllocation a) {
        return new AllocationDto(a.getId(), a.getEquipmentItemId(), a.getEquipmentName(), a.getCaseId(),
                a.getCaseNumber(), a.getQuantity(), a.isReturned(), a.getReturnedAt(), a.getCreatedAt());
    }
}
