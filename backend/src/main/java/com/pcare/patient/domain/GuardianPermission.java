package com.pcare.patient.domain;

/** Granular permissions a patient grants to a guardian/family member (blueprint point 54). */
public enum GuardianPermission {
    VIEW_LOCATION,
    VIEW_RECORDS,
    RECEIVE_NOTIFICATIONS,
    APPROVE_SERVICES,
    MAKE_PAYMENTS
}
