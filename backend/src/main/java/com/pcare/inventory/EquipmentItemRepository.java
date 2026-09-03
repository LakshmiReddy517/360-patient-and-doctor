package com.pcare.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, Long> {

    List<EquipmentItem> findByActiveTrueOrderByNameAsc();

    List<EquipmentItem> findByCategory(EquipmentCategory category);

    Optional<EquipmentItem> findFirstByNameIgnoreCase(String name);
}
