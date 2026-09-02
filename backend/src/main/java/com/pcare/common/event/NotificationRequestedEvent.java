package com.pcare.common.event;

/**
 * A domain event asking the notification engine to send a message. Publishing modules stay
 * decoupled from the notification implementation — they just raise this event.
 */
public record NotificationRequestedEvent(
        Long recipientUserId,
        String recipientName,
        String recipientAddress,
        String channel,
        String category,
        String subject,
        String body,
        Long caseId
) {
}
