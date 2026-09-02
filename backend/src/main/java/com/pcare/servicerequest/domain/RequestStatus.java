package com.pcare.servicerequest.domain;

import java.util.Set;

/**
 * Controlled service-request lifecycle (blueprint point 7):
 * New → Under Review → Information Required → Triage → Quote Prepared → Quote Sent → Accepted →
 * Payment Pending → Confirmed → Resource Assignment → In Progress → Completed → Closed,
 * with Cancelled, Rejected, Expired, On Hold and Emergency Escalated exceptions.
 */
public enum RequestStatus {
    NEW,
    UNDER_REVIEW,
    INFO_REQUIRED,
    TRIAGE,
    QUOTE_PREPARED,
    QUOTE_SENT,
    ACCEPTED,
    PAYMENT_PENDING,
    CONFIRMED,
    RESOURCE_ASSIGNMENT,
    IN_PROGRESS,
    COMPLETED,
    CLOSED,
    // exceptions
    CANCELLED,
    REJECTED,
    EXPIRED,
    ON_HOLD,
    EMERGENCY_ESCALATED;

    /** Allowed forward/exception transitions. Exceptions are reachable from most active states. */
    private static final Set<RequestStatus> EXCEPTIONS =
            Set.of(CANCELLED, REJECTED, EXPIRED, ON_HOLD, EMERGENCY_ESCALATED);

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED || this == REJECTED || this == EXPIRED;
    }

    public boolean canTransitionTo(RequestStatus next) {
        if (this == next) return false;
        if (isTerminal()) return false;
        if (EXCEPTIONS.contains(next)) return true;              // exceptions allowed from any active state
        // ON_HOLD and EMERGENCY_ESCALATED are recoverable — an operator can resume the journey
        // into any active (non-exception) state.
        if (this == ON_HOLD || this == EMERGENCY_ESCALATED) {
            return next.ordinal() <= CLOSED.ordinal();
        }
        return next.ordinal() > this.ordinal() && next.ordinal() <= CLOSED.ordinal();
    }
}
