package com.pcare.support.domain;

/** Enumerations for the complaints / incidents / SLA module (blueprint points 68, 77, 78). */
public final class SupportEnums {

    private SupportEnums() {
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum ComplaintStatus {
        OPEN, ASSIGNED, INVESTIGATING, RESOLVED, CLOSED, REOPENED;

        public boolean isTerminal() {
            return this == CLOSED;
        }
    }

    public enum IncidentType {
        PATIENT_UNAVAILABLE, WRONG_LOCATION, VEHICLE_BREAKDOWN, PATIENT_DETERIORATION,
        ADMISSION_ISSUE, APPOINTMENT_CANCELLATION, PAYMENT_FAILURE, DOCUMENT_GAP,
        SERVICE_COMPLAINT, OTHER
    }

    public enum IncidentSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum IncidentStatus {
        OPEN, IN_PROGRESS, ESCALATED, RESOLVED, CLOSED
    }
}
