package com.pcare.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.pcare.notification.domain.DeviceToken;
import com.pcare.notification.repo.DeviceTokenRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging push sender. Initialises from a service-account JSON given by
 * {@code app.firebase.credentials-path} (or the FIREBASE_CREDENTIALS env var). If no credentials are
 * configured the service stays gracefully DISABLED — every send is a no-op — so the app runs fine
 * until the Firebase service-account file is dropped in. Then real push "just works".
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    private boolean enabled = false;

    public PushService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostConstruct
    void init() {
        String path = (credentialsPath != null && !credentialsPath.isBlank())
                ? credentialsPath : System.getenv("FIREBASE_CREDENTIALS");
        if (path == null || path.isBlank()) {
            log.info("Firebase push DISABLED — no service-account configured (app.firebase.credentials-path / FIREBASE_CREDENTIALS). "
                    + "Notifications are still recorded; add the credentials file to enable device push.");
            return;
        }
        try (FileInputStream in = new FileInputStream(path)) {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(in))
                        .build());
            }
            enabled = true;
            log.info("Firebase push ENABLED (credentials: {})", path);
        } catch (Exception e) {
            log.warn("Firebase push could not initialise from {}: {} — push disabled.", path, e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Push to every device of the recipient, matching on userId OR patientId. */
    public void pushToRecipient(Long recipientId, String title, String body, Long caseId) {
        if (recipientId == null) return;
        send(deviceTokenRepository.findByUserIdOrPatientId(recipientId, recipientId), title, body, caseId);
    }

    /** Push to every device registered to a specific user (e.g. an agent for a new job). */
    public void pushToUser(Long userId, String title, String body, Long caseId) {
        if (userId == null) return;
        send(deviceTokenRepository.findByUserId(userId), title, body, caseId);
    }

    private void send(List<DeviceToken> tokens, String title, String body, Long caseId) {
        if (!enabled || tokens == null || tokens.isEmpty()) return;
        for (DeviceToken t : tokens) {
            try {
                Message msg = Message.builder()
                        .setToken(t.getToken())
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putAllData(Map.of(
                                "caseId", caseId == null ? "" : String.valueOf(caseId),
                                "title", title == null ? "" : title))
                        .build();
                FirebaseMessaging.getInstance().send(msg);
            } catch (Exception e) {
                String m = e.getMessage() == null ? "" : e.getMessage();
                // Prune tokens the FCM service reports as no-longer-valid so the table stays clean.
                if (m.contains("registration-token-not-registered") || m.contains("Requested entity was not found")
                        || m.contains("UNREGISTERED") || m.contains("INVALID_ARGUMENT")) {
                    try { deviceTokenRepository.delete(t); } catch (Exception ignored) { }
                }
                log.debug("FCM send failed for token {}: {}", t.getId(), m);
            }
        }
    }
}
