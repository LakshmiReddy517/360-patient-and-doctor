package com.pcare.servicerequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Pickup details (blueprint point 9), embedded in a service request. */
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class PickupInfo {

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_source", length = 30)
    private PickupSource source;

    @Column(name = "pickup_address", length = 400)
    private String address;

    @Column(name = "pickup_lat")
    private Double latitude;

    @Column(name = "pickup_lng")
    private Double longitude;

    @Column(name = "pickup_datetime")
    private LocalDateTime scheduledAt;

    // Travel details for airport/railway pickups
    @Column(name = "pickup_flight_train_no", length = 40)
    private String flightOrTrainNumber;

    @Column(name = "pickup_arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "pickup_terminal", length = 60)
    private String terminalOrCoach;

    @Column(name = "pickup_seat", length = 40)
    private String seatOrBerth;
}
