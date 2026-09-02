package com.pcare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds {@code app.cors.*}. */
@Component
@ConfigurationProperties(prefix = "app.cors")
public class AppCorsProperties {

    private String allowedOrigins = "http://localhost:5173";

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
