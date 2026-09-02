package com.pcare.healthcare.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/** Hospital master (blueprint point 39): departments, services, emergency/OPD, contacts, partner. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "hospital", indexes = {
        @Index(name = "ix_hospital_city", columnList = "city"),
        @Index(name = "ix_hospital_name", columnList = "name")
})
public class Hospital extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 400)
    private String addressLine;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(length = 30)
    private String phone;

    @Column(length = 160)
    private String email;

    /** Geo-coordinates for "find nearby" search from the patient app's device GPS. */
    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private boolean emergencyAvailable = false;

    @Column(length = 120)
    private String opdTiming;

    @Column(nullable = false)
    private boolean partner = false;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_department", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "department", length = 120)
    private Set<String> departments = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_service", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "service_name", length = 120)
    private Set<String> services = new HashSet<>();
}
