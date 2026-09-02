package com.pcare.notification.domain;

import com.pcare.common.domain.BaseEntity;
import com.pcare.notification.domain.NotificationEnums.Category;
import com.pcare.notification.domain.NotificationEnums.Channel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-user preference for one category (blueprint point 48): which channels are permitted, and
 * whether the category is enabled at all. Subject to consent and policy; EMERGENCY always delivers.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_preference", indexes = {
        @Index(name = "ux_pref_user_cat", columnList = "userId,category", unique = true)
})
public class NotificationPreference extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_preference_channel", joinColumns = @JoinColumn(name = "preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private Set<Channel> channels = new HashSet<>();
}
