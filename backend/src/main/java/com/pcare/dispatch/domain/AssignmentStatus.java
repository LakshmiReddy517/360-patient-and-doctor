package com.pcare.dispatch.domain;

/** Pickup/handover workflow states for an assignment (blueprint point 23). */
public enum AssignmentStatus {
    OFFERED,
    ACCEPTED,
    EN_ROUTE,
    ARRIVED_AT_PICKUP,
    PATIENT_VERIFIED,
    PATIENT_PICKED,
    IN_TRANSIT,
    HOSPITAL_ARRIVED,
    HANDED_OVER,
    COMPLETED,
    CANCELLED;

    public boolean isClosed() {
        return this == COMPLETED || this == CANCELLED;
    }
}
