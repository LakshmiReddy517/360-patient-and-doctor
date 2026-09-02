package com.pcare.fleet.domain;

/** Ambulance duty/trip status used for live tracking (blueprint points 24, 30). */
public enum AmbulanceStatus {
    OFFLINE,
    AVAILABLE,
    EN_ROUTE,
    AT_PICKUP,
    TRANSPORTING,
    AT_HOSPITAL,
    MAINTENANCE,
    OUT_OF_SERVICE;

    public boolean isDispatchable() {
        return this == AVAILABLE;
    }
}
