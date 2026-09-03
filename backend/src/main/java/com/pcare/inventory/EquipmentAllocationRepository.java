package com.pcare.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentAllocationRepository extends JpaRepository<EquipmentAllocation, Long> {

    List<EquipmentAllocation> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    List<EquipmentAllocation> findByEquipmentItemIdAndReturnedOrderByCreatedAtDesc(Long itemId, boolean returned);
}
