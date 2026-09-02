package com.pcare.healthcare.domain;

/** Doctor appointment workflow (blueprint point 41). */
public enum AppointmentStatus {
    REQUESTED,
    SLOT_RESERVED,
    CONFIRMED,
    REMINDED,
    ARRIVED,
    IN_CONSULTATION,
    PRESCRIBED,
    COMPLETED,
    FOLLOW_UP,
    CANCELLED,
    NO_SHOW;

    public boolean isClosed() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}
