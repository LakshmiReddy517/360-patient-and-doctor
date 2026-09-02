package com.pcare.billing.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A configurable service package (blueprint point 16) composed of reusable items. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "service_package")
public class ServicePackage extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 600)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PackageCategory category = PackageCategory.CUSTOM;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_package_item", joinColumns = @JoinColumn(name = "package_id"))
    @OrderColumn(name = "item_order")
    private List<PackageItem> items = new ArrayList<>();
}
