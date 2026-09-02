package com.pcare.notification.domain;

/** Enumerations for the notification engine (blueprint points 47, 48). */
public final class NotificationEnums {

    private NotificationEnums() {
    }

    public enum Channel {
        WHATSAPP, SMS, EMAIL, PUSH, INTERNAL
    }

    public enum Category {
        APPOINTMENT, PAYMENT, PICKUP, MEDICAL, MEDICATION, EMERGENCY, MARKETING, GENERAL
    }

    public enum DeliveryStatus {
        QUEUED, SENT, DELIVERED, FAILED, READ, SUPPRESSED
    }
}
