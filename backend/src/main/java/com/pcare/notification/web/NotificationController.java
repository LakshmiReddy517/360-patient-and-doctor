package com.pcare.notification.web;

import com.pcare.notification.domain.NotificationEnums.Category;
import com.pcare.notification.domain.NotificationEnums.Channel;
import com.pcare.notification.domain.NotificationEnums.DeliveryStatus;
import com.pcare.notification.service.NotificationService;
import com.pcare.notification.service.PreferenceService;
import com.pcare.notification.web.dto.NotificationDtos.NotificationDto;
import com.pcare.notification.web.dto.NotificationDtos.NotificationStats;
import com.pcare.notification.web.dto.NotificationDtos.PreferenceDto;
import com.pcare.notification.web.dto.NotificationDtos.SendRequest;
import com.pcare.notification.web.dto.NotificationDtos.UpsertPreferenceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notifications", description = "Event-driven notifications, delivery history and preferences")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final PreferenceService preferenceService;

    public NotificationController(NotificationService notificationService, PreferenceService preferenceService) {
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
    }

    @Operation(summary = "Notification delivery stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/stats")
    public NotificationStats stats() {
        return notificationService.stats();
    }

    @Operation(summary = "Search notification history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping
    public Page<NotificationDto> search(@RequestParam(required = false) Channel channel,
                                        @RequestParam(required = false) Category category,
                                        @RequestParam(required = false) DeliveryStatus status,
                                        @PageableDefault(size = 30) Pageable pageable) {
        return notificationService.search(channel, category, status, pageable);
    }

    @Operation(summary = "Notifications for a case")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/case/{caseId}")
    public List<NotificationDto> byCase(@PathVariable Long caseId) {
        return notificationService.byCase(caseId);
    }

    @Operation(summary = "Send a notification (applies preferences / consent gating)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping
    public NotificationDto send(@Valid @RequestBody SendRequest req) {
        return notificationService.toDto(notificationService.send(req));
    }

    @Operation(summary = "Mark a notification read")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PATIENT')")
    @PostMapping("/{id}/read")
    public NotificationDto markRead(@PathVariable Long id) {
        return notificationService.toDto(notificationService.markRead(id));
    }

    // ---- Preferences ----
    @Operation(summary = "Get a user's notification preferences")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PATIENT')")
    @GetMapping("/preferences/{userId}")
    public List<PreferenceDto> preferences(@PathVariable Long userId) {
        return preferenceService.forUser(userId);
    }

    @Operation(summary = "Create/update a preference")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PATIENT')")
    @PutMapping("/preferences")
    public PreferenceDto upsert(@Valid @RequestBody UpsertPreferenceRequest req) {
        return preferenceService.upsert(req);
    }
}
