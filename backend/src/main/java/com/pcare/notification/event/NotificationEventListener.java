package com.pcare.notification.event;

import com.pcare.common.event.NotificationRequestedEvent;
import com.pcare.notification.domain.NotificationEnums.Category;
import com.pcare.notification.domain.NotificationEnums.Channel;
import com.pcare.notification.service.NotificationService;
import com.pcare.notification.web.dto.NotificationDtos.SendRequest;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns {@link NotificationRequestedEvent}s raised anywhere in the app into actual notifications.
 * Runs synchronously within the publisher's transaction, so a notification is only persisted if the
 * originating work commits, and publishers stay fully decoupled from the notification engine.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onNotificationRequested(NotificationRequestedEvent e) {
        notificationService.send(new SendRequest(
                e.recipientUserId(), e.recipientName(), e.recipientAddress(),
                Channel.valueOf(e.channel()), Category.valueOf(e.category()),
                e.subject(), e.body(), e.caseId()));
    }
}
