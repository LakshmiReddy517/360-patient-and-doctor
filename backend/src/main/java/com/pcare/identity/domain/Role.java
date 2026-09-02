package com.pcare.identity.domain;

/**
 * Platform roles from the blueprint's application ecosystem. Spring Security authorities are
 * derived as {@code ROLE_<name>}. Access to medical data is least-privilege and further gated
 * by consent at the service layer — a role alone never exposes protected health information.
 */
public enum Role {
    /** Full platform administration and configuration. */
    SUPER_ADMIN,
    /** Admin / Command Centre operator: requests, triage, dispatch, cases. */
    ADMIN,
    /** Finance / billing: invoices, payments, settlements. */
    FINANCE,
    /** Field care agent handling pickup/drop and coordination. */
    AGENT,
    /** Ambulance driver / Emergency Medical Technician. */
    EMT,
    /** Doctor / clinician (consent-aware clinical actions). */
    DOCTOR,
    /** Hospital / partner portal user. */
    HOSPITAL,
    /** Caretaker providing longer-duration patient care. */
    CARETAKER,
    /** Patient or guardian using the Patient App. */
    PATIENT;

    public String authority() {
        return "ROLE_" + name();
    }
}
