package com.pcare.servicerequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Destination / hospital details (blueprint point 10), embedded in a service request. */
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class DestinationInfo {

    @Column(name = "dest_type", length = 60)
    private String destinationType;   // e.g. Hospital, Home, Hotel

    /** FK to the Hospital Master (blueprint point 10) — when set, name/coords are filled from master. */
    @Column(name = "dest_hospital_id")
    private Long hospitalId;

    @Column(name = "dest_hospital", length = 160)
    private String hospitalName;

    @Column(name = "dest_department", length = 120)
    private String department;

    @Column(name = "dest_doctor", length = 120)
    private String doctorName;

    @Column(name = "dest_address", length = 400)
    private String address;

    @Column(name = "dest_lat")
    private Double latitude;

    @Column(name = "dest_lng")
    private Double longitude;

    @Column(name = "dest_appointment_at")
    private LocalDateTime appointmentAt;
}
