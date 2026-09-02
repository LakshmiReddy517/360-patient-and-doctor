package com.pcare.notification.repo;

import com.pcare.notification.domain.NotificationEnums.Category;
import com.pcare.notification.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndCategory(Long userId, Category category);
}
