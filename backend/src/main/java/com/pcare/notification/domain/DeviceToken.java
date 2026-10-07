package com.pcare.notification.domain;

import com.pcare.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An FCM (Firebase Cloud Messaging) registration token for one user's device, used to deliver
 * push notifications for every case event. Stored per user; the patientId is cached so patient-facing
 * events (which reference the patient entity) can resolve the device without an extra lookup.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "device_token", indexes = {
        @Index(name = "ux_device_token", columnList = "token", unique = true),
        @Index(name = "ix_device_user", columnList = "userId"),
        @Index(name = "ix_device_patient", columnList = "patientId")
})
public class DeviceToken extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    /** The patient entity linked to this user, if any (so patient-facing pushes resolve directly). */
    private Long patientId;

    @Column(nullable = false, length = 400)
    private String token;

    @Column(length = 20)
    private String platform = "android";
}
