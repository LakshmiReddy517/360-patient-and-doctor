package com.pcare.care.domain;

/** Enumerations for the care-services module, grouped for brevity. */
public final class CareEnums {

    private CareEnums() {
    }

    /** Lifecycle shared by caretaker assignments and care bookings. */
    public enum CareStatus {
        REQUESTED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    /** Type of a unified care booking (blueprint points 42, 43, 44). */
    public enum CareServiceType {
        ACCOMMODATION, FOOD, LOCAL_TRANSPORT
    }

    /** Entries in the caretaker daily activity diary (blueprint point 37). */
    public enum CareActivityType {
        MEDICINE, MEAL, VITALS, DOCTOR_VISIT, REST, OBSERVATION, HANDOVER
    }
}
