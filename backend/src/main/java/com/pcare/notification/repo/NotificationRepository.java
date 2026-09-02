package com.pcare.notification.repo;

import com.pcare.notification.domain.Notification;
import com.pcare.notification.domain.NotificationEnums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    long countByStatus(DeliveryStatus status);

    @Query("select n from Notification n where "
            + "(:channel is null or n.channel = :channel) and (:category is null or n.category = :category) "
            + "and (:status is null or n.status = :status) order by n.createdAt desc")
    Page<Notification> search(@Param("channel") com.pcare.notification.domain.NotificationEnums.Channel channel,
                              @Param("category") com.pcare.notification.domain.NotificationEnums.Category category,
                              @Param("status") DeliveryStatus status,
                              Pageable pageable);
}
