package com.pcare.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Helpers for reading the current authenticated principal. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AppUserPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentPrincipal().map(AppUserPrincipal::getUserId);
    }

    public static Optional<String> currentUsername() {
        return currentPrincipal().map(AppUserPrincipal::getUsername);
    }
}
