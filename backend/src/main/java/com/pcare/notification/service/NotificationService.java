package com.pcare.notification.service;

import com.pcare.common.exception.NotFoundException;
import com.pcare.notification.domain.Notification;
import com.pcare.notification.domain.NotificationEnums.Category;
import com.pcare.notification.domain.NotificationEnums.Channel;
import com.pcare.notification.domain.NotificationEnums.DeliveryStatus;
import com.pcare.notification.domain.NotificationPreference;
import com.pcare.notification.repo.NotificationPreferenceRepository;
import com.pcare.notification.repo.NotificationRepository;
import com.pcare.notification.web.dto.NotificationDtos.NotificationDto;
import com.pcare.notification.web.dto.NotificationDtos.NotificationStats;
import com.pcare.notification.web.dto.NotificationDtos.SendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Event-driven notification engine. Applies per-user channel/category preferences and consent
 * policy, then "dispatches" via the requested channel (mock delivery in this build) and records
 * the full communication history.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final com.pcare.live.LiveHub liveHub;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferenceRepository preferenceRepository,
                               com.pcare.live.LiveHub liveHub) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.liveHub = liveHub;
    }

    @Transactional
    public Notification send(SendRequest req) {
        Notification n = new Notification();
        n.setRecipientUserId(req.recipientUserId());
        n.setRecipientName(req.recipientName());
        n.setRecipientAddress(req.recipientAddress());
        n.setChannel(req.channel());
        n.setCategory(req.category());
        n.setSubject(req.subject());
        n.setBody(req.body());
        n.setCaseId(req.caseId());

        if (!isAllowed(req.recipientUserId(), req.category(), req.channel())) {
            n.setStatus(DeliveryStatus.SUPPRESSED);
            n.setErrorMessage("Suppressed by recipient notification preferences / consent policy");
            return notificationRepository.save(n);
        }

        // Mock dispatch — in production this calls the WhatsApp/SMS/email/push provider.
        n.setStatus(DeliveryStatus.SENT);
        n.setSentAt(Instant.now());
        Notification saved = notificationRepository.save(n);
        saved.setStatus(DeliveryStatus.DELIVERED);
        saved.setDeliveredAt(Instant.now());
        log.debug("Notification {} delivered via {} to {}", saved.getId(), saved.getChannel(), saved.getRecipientName());
        Notification result = notificationRepository.save(saved);
        liveHub.broadcast("notification", java.util.Map.of(
                "channel", result.getChannel().name(), "category", result.getCategory().name(),
                "subject", result.getSubject() == null ? "" : result.getSubject(),
                "recipient", result.getRecipientName() == null ? "" : result.getRecipientName(),
                "status", result.getStatus().name()));
        return result;
    }

    /**
     * Gating: EMERGENCY always delivers. MARKETING requires explicit opt-in (a preference that is
     * enabled and includes the channel). Other categories default to allowed unless a preference
     * disables them or restricts channels.
     */
    private boolean isAllowed(Long userId, Category category, Channel channel) {
        if (category == Category.EMERGENCY) {
            return true;
        }
        if (userId == null) {
            return category != Category.MARKETING; // no recipient account -> don't send marketing
        }
        Optional<NotificationPreference> prefOpt = preferenceRepository.findByUserIdAndCategory(userId, category);
        if (category == Category.MARKETING) {
            return prefOpt.map(p -> p.isEnabled() && p.getChannels().contains(channel)).orElse(false);
        }
        if (prefOpt.isEmpty()) {
            return true; // default allow
        }
        NotificationPreference pref = prefOpt.get();
        if (!pref.isEnabled()) {
            return false;
        }
        return pref.getChannels().isEmpty() || pref.getChannels().contains(channel);
    }

    @Transactional
    public Notification markRead(Long id) {
        Notification n = notificationRepository.findById(id).orElseThrow(() -> NotFoundException.of("Notification", id));
        n.setStatus(DeliveryStatus.READ);
        n.setReadAt(Instant.now());
        return notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> search(Channel channel, Category category, DeliveryStatus status, Pageable pageable) {
        return notificationRepository.search(channel, category, status, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> byCase(Long caseId) {
        return notificationRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public NotificationStats stats() {
        return new NotificationStats(
                notificationRepository.count(),
                notificationRepository.countByStatus(DeliveryStatus.DELIVERED)
                        + notificationRepository.countByStatus(DeliveryStatus.READ),
                notificationRepository.countByStatus(DeliveryStatus.FAILED),
                notificationRepository.countByStatus(DeliveryStatus.SUPPRESSED));
    }

    public NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getRecipientUserId(), n.getRecipientName(), n.getRecipientAddress(),
                n.getChannel(), n.getCategory(), n.getSubject(), n.getBody(), n.getCaseId(), n.getStatus(),
                n.getSentAt(), n.getDeliveredAt(), n.getReadAt(), n.getErrorMessage(), n.getCreatedAt());
    }
}
