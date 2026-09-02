package com.pcare.config;

import com.pcare.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Supplies the current username to {@code @CreatedBy}/{@code @LastModifiedBy}. This is how the
 * blueprint's requirement that audit metadata be filled automatically (never left blank) is met.
 */
@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(SecurityUtils.currentUsername().orElse("system"));
    }
}
