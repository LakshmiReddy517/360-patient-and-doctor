package com.pcare.fleet.domain;

/**
 * Operational ambulance categories (blueprint point 25). Clinical capability matters, so we use
 * these rather than a simplistic Small/Big classification.
 */
public enum AmbulanceCategory {
    /** Patient Transport Vehicle — basic, non-emergency transport. */
    PATIENT_TRANSPORT,
    /** Basic Life Support. */
    BLS,
    /** Advanced Life Support. */
    ALS
}
