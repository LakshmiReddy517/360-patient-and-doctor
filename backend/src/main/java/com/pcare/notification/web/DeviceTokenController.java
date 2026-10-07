package com.pcare.notification.web;

import com.pcare.notification.domain.DeviceToken;
import com.pcare.notification.repo.DeviceTokenRepository;
import com.pcare.patient.repo.PatientRepository;
import com.pcare.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registers the calling device's FCM token so it can receive push notifications (blueprint points 47/48). */
@Tag(name = "Device Tokens", description = "FCM push-notification device registration")
@RestController
@RequestMapping("/api/v1/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenRepository repo;
    private final PatientRepository patientRepository;

    public DeviceTokenController(DeviceTokenRepository repo, PatientRepository patientRepository) {
        this.repo = repo;
        this.patientRepository = patientRepository;
    }

    public record RegisterTokenRequest(@NotBlank String token, String platform) {
    }

    @Operation(summary = "Register this device's push token for the logged-in user")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @Transactional
    public java.util.Map<String, Object> register(@RequestBody RegisterTokenRequest req) {
        Long uid = SecurityUtils.currentUserId()
                .orElseThrow(() -> new com.pcare.common.exception.NotFoundException("Not authenticated"));
        Long patientId = patientRepository.findByUserId(uid).map(p -> p.getId()).orElse(null);
        DeviceToken dt = repo.findByToken(req.token()).orElseGet(DeviceToken::new);
        dt.setUserId(uid);
        dt.setPatientId(patientId);
        dt.setToken(req.token());
        if (req.platform() != null && !req.platform().isBlank()) dt.setPlatform(req.platform());
        repo.save(dt);
        return java.util.Map.of("registered", true, "userId", uid);
    }
}
