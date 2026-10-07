package com.pcare.notification.repo;

import com.pcare.notification.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserId(Long userId);

    List<DeviceToken> findByPatientId(Long patientId);

    /** All tokens whose userId OR patientId matches the given id — covers both targeting styles. */
    List<DeviceToken> findByUserIdOrPatientId(Long userId, Long patientId);
}
