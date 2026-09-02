package com.pcare.notification.web.dto;

import com.pcare.notification.domain.NotificationEnums.Category;
import com.pcare.notification.domain.NotificationEnums.Channel;
import com.pcare.notification.domain.NotificationEnums.DeliveryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

/** DTOs for the notification engine. */
public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record SendRequest(
            Long recipientUserId, String recipientName, String recipientAddress,
            @NotNull Channel channel, @NotNull Category category,
            @NotBlank String subject, String body, Long caseId) {
    }

    public record NotificationDto(
            Long id, Long recipientUserId, String recipientName, String recipientAddress,
            Channel channel, Category category, String subject, String body, Long caseId,
            DeliveryStatus status, Instant sentAt, Instant deliveredAt, Instant readAt,
            String errorMessage, Instant createdAt) {
    }

    public record PreferenceDto(Long id, Long userId, Category category, boolean enabled, Set<Channel> channels) {
    }

    public record UpsertPreferenceRequest(
            @NotNull Long userId, @NotNull Category category, boolean enabled, Set<Channel> channels) {
    }

    public record NotificationStats(long total, long delivered, long failed, long suppressed) {
    }
}
