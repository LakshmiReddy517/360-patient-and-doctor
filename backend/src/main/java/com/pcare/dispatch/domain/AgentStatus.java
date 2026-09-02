package com.pcare.dispatch.domain;

/** Care-agent availability/duty status (blueprint point 20). */
public enum AgentStatus {
    OFFLINE,
    ONLINE,
    AVAILABLE,
    JOB_OFFERED,
    ACCEPTED,
    EN_ROUTE,
    ARRIVED,
    PATIENT_PICKED,
    IN_TRANSIT,
    HOSPITAL_ARRIVED,
    JOB_COMPLETED,
    ON_BREAK,
    SUSPENDED;

    /** Whether an agent in this status can be offered new work. */
    public boolean isDispatchable() {
        return this == ONLINE || this == AVAILABLE;
    }
}
