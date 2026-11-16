package com.pcare.notification.otp;

import com.pcare.common.exception.BadRequestException;
import com.pcare.notification.service.SmsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Generates, delivers (via {@link SmsService}) and verifies one-time passcodes.
 * Codes are random 6-digit, expire in 5 minutes, allow 5 verify attempts, are one-time-use, and
 * are stored only as a SHA-256 hash. Requests are rate-limited per mobile.
 */
@Service
public class OtpService {

    private static final int EXPIRY_MIN = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RATE_WINDOW_MIN = 60;
    private static final int RATE_MAX = 5;

    private final OtpRepository repo;
    private final SmsService sms;
    private final SecureRandom rnd = new SecureRandom();

    public OtpService(OtpRepository repo, SmsService sms) {
        this.repo = repo;
        this.sms = sms;
    }

    @Transactional
    public void requestOtp(String mobile, String purpose) {
        String m = clean(mobile);
        if (m == null) throw new BadRequestException("Enter a valid 10-digit mobile number.");
        String p = normalizePurpose(purpose);

        long recent = repo.countByMobileAndCreatedAtAfter(m, Instant.now().minus(RATE_WINDOW_MIN, ChronoUnit.MINUTES));
        if (recent >= RATE_MAX) throw new BadRequestException("Too many OTP requests. Please try again later.");

        String code = String.format("%06d", rnd.nextInt(1_000_000));
        OtpToken t = new OtpToken();
        t.setMobile(m);
        t.setPurpose(p);
        t.setCodeHash(hash(code));
        t.setExpiresAt(Instant.now().plus(EXPIRY_MIN, ChronoUnit.MINUTES));
        repo.save(t);

        sms.sendOtp(m, code, p.toLowerCase());
    }

    @Transactional
    public boolean verifyOtp(String mobile, String purpose, String code) {
        String m = clean(mobile);
        if (m == null || code == null || code.isBlank()) return false;
        String p = normalizePurpose(purpose);

        OtpToken t = repo.findFirstByMobileAndPurposeAndUsedFalseOrderByCreatedAtDesc(m, p).orElse(null);
        if (t == null) return false;
        if (t.getExpiresAt().isBefore(Instant.now()) || t.getAttempts() >= MAX_ATTEMPTS) return false;

        t.setAttempts(t.getAttempts() + 1);
        boolean ok = constantTimeEquals(t.getCodeHash(), hash(code.trim()));
        if (ok) t.setUsed(true);
        repo.save(t);
        return ok;
    }

    private static String normalizePurpose(String purpose) {
        return (purpose == null || purpose.isBlank()) ? "LOGIN" : purpose.trim().toUpperCase();
    }

    /** Keep the last 10 digits — tolerant of +91 / 0 prefixes. */
    private static String clean(String mobile) {
        if (mobile == null) return null;
        String d = mobile.replaceAll("\\D", "");
        if (d.length() < 10) return null;
        return d.substring(d.length() - 10);
    }

    private static String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
