package com.pcare.accommodation;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A place a patient's family can stay near a treating hospital (blueprint point 42).
 * Curated accommodation master — hotels, guest houses, service apartments and dharamshalas —
 * with nightly price, availability and a reference walking distance to the hospital.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "accommodation_option", indexes = {
        @Index(name = "ix_accommodation_city", columnList = "city"),
        @Index(name = "ix_accommodation_available", columnList = "available")
})
public class AccommodationOption extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 40)
    private String type;

    @Column(length = 400)
    private String addressLine;

    @Column(length = 120)
    private String city;

    private Double latitude;

    private Double longitude;

    @Column(length = 80)
    private String roomType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight = BigDecimal.ZERO;

    private Double distanceToHospitalKm;

    @Column(nullable = false)
    private boolean available = true;

    private Integer roomsAvailable;

    @Column(length = 30)
    private String contactPhone;

    @Column(nullable = false)
    private boolean active = true;
}
