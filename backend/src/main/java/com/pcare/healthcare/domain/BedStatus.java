package com.pcare.healthcare.domain;

/** Bed availability lifecycle (blueprint point 40). */
public enum BedStatus {
    AVAILABLE,
    RESERVED,
    OCCUPIED,
    CLEANING,
    MAINTENANCE,
    BLOCKED
}
