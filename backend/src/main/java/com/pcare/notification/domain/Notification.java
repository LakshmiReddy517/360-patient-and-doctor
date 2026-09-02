package com.pcare.notification.domain;

import com.pcare.common.domain.BaseEntity;
import com.pcare.notification.domain.NotificationEnums.Category;
import com.pcare.notification.domain.NotificationEnums.Channel;
import com.pcare.notification.domain.NotificationEnums.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A single notification and its delivery record — the communication history (blueprint point 47). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "ix_notif_recipient", columnList = "recipientUserId"),
        @Index(name = "ix_notif_case", columnList = "caseId"),
        @Index(name = "ix_notif_status", columnList = "status")
})
public class Notification extends BaseEntity {

    private Long recipientUserId;

    @Column(length = 160)
    private String recipientName;

    @Column(length = 160)
    private String recipientAddress; // mobile / email / device token

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category = Category.GENERAL;

    @Column(length = 200)
    private String subject;

    @Column(length = 1000)
    private String body;

    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status = DeliveryStatus.QUEUED;

    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;

    @Column(length = 300)
    private String errorMessage;
}
