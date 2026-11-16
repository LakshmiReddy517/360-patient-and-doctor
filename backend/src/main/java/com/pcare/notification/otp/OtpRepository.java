package com.pcare.notification.otp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findFirstByMobileAndPurposeAndUsedFalseOrderByCreatedAtDesc(String mobile, String purpose);

    long countByMobileAndCreatedAtAfter(String mobile, Instant after);
}
