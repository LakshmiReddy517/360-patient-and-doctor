package com.pcare.casefile.domain;

/**
 * Overall lifecycle state of a Case (the end-to-end service journey). The fine-grained
 * request lifecycle (New, Triage, Quote, ...) lives on the ServiceRequest; the Case rolls
 * those up into an operational state the Command Centre acts on.
 */
public enum CaseStatus {
    DRAFT,        // created, not yet actioned
    OPEN,         // active and being coordinated
    IN_PROGRESS,  // resources assigned / work underway
    ON_HOLD,      // paused (waiting on info, payment, patient)
    COMPLETED,    // all committed services delivered
    CLOSED,       // finalised and billed
    CANCELLED     // stopped before completion
}
