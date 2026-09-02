package com.pcare.patient.domain;

/** Categories of medical document (blueprint point 14). */
public enum DocumentType {
    ID_PROOF,
    PRESCRIPTION,
    LAB_REPORT,
    IMAGING,            // CT / MRI / X-ray
    DISCHARGE_SUMMARY,
    MEDICAL_CERTIFICATE,
    INSURANCE_DOCUMENT,
    OTHER
}
