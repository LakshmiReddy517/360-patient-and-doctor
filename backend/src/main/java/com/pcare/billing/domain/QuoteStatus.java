package com.pcare.billing.domain;

/** Quote lifecycle (blueprint point 18). An accepted quote acts as the invoice for payments. */
public enum QuoteStatus {
    DRAFT,
    SENT,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    PAID
}
