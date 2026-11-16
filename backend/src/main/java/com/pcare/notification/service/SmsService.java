package com.pcare.notification.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Real-time SMS / OTP delivery via MSG91 (India DLT-compliant gateway).
 *
 * <p>Uses the MSG91 Flow API to deliver a specific, server-generated OTP through an approved DLT
 * template. We control the OTP (generate + verify ourselves); MSG91 only delivers the message.</p>
 *
 * <p>Gracefully DISABLED until configured — if {@code msg91.authkey} / {@code msg91.otp-template-id}
 * are blank, every send is a safe no-op (the code is logged in dev) so the app runs fine without
 * credentials. Set the env vars and real SMS "just works".</p>
 *
 * Config (env vars or application.yml):
 *   msg91.authkey            (MSG91_AUTHKEY)        — your MSG91 auth key
 *   msg91.otp-template-id    (MSG91_OTP_TEMPLATE)   — approved DLT OTP template id
 *   msg91.var-name           (default "otp")        — the template variable holding the code
 *   msg91.country-code       (default "91")         — dialing code prefix
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final String FLOW_URL = "https://control.msg91.com/api/v5/flow/";

    @Value("${msg91.authkey:}")
    private String authKey;
    @Value("${msg91.otp-template-id:}")
    private String otpTemplateId;
    @Value("${msg91.var-name:otp}")
    private String varName;
    @Value("${msg91.country-code:91}")
    private String countryCode;

    private boolean enabled = false;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    @PostConstruct
    void init() {
        String key = firstNonBlank(authKey, System.getenv("MSG91_AUTHKEY"));
        String tpl = firstNonBlank(otpTemplateId, System.getenv("MSG91_OTP_TEMPLATE"));
        if (isBlank(key) || isBlank(tpl)) {
            log.info("SMS/OTP delivery DISABLED — MSG91 not configured (msg91.authkey / msg91.otp-template-id). "
                    + "OTPs are still generated & verified; add the MSG91 credentials to deliver real SMS.");
            return;
        }
        this.authKey = key;
        this.otpTemplateId = tpl;
        this.enabled = true;
        log.info("SMS/OTP delivery ENABLED via MSG91 (template {}).", tpl);
    }

    public boolean isEnabled() { return enabled; }

    /** Deliver a server-generated OTP to a patient/user mobile. Safe no-op when disabled. */
    public void sendOtp(String mobile, String code, String purpose) {
        String to = normalize(mobile);
        if (to == null) { log.warn("SMS OTP skipped — invalid mobile '{}'", mobile); return; }
        if (!enabled) {
            // Dev fallback so the flow is testable without a gateway. NEVER do this in production logs.
            log.info("[SMS DISABLED] OTP for {} ({}) would be: {}", to, purpose, code);
            return;
        }
        String body = "{\"template_id\":\"" + esc(otpTemplateId) + "\",\"short_url\":\"0\","
                + "\"recipients\":[{\"mobiles\":\"" + to + "\",\"" + esc(varName) + "\":\"" + esc(code) + "\"}]}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(FLOW_URL))
                    .header("authkey", authKey)
                    .header("Content-Type", "application/json")
                    .header("accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 == 2) {
                log.info("SMS OTP sent to {} ({})", mask(to), purpose);
            } else {
                log.warn("MSG91 OTP send failed ({}): {}", res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.warn("MSG91 OTP send error for {}: {}", mask(to), e.getMessage());
        }
    }

    // ---- helpers ----
    private String normalize(String mobile) {
        if (mobile == null) return null;
        String d = mobile.replaceAll("\\D", "");
        if (d.length() == 10) d = countryCode + d;
        else if (d.length() == 11 && d.startsWith("0")) d = countryCode + d.substring(1);
        return d.length() >= 11 ? d : null;
    }
    private static String mask(String m) { return m == null || m.length() < 4 ? "****" : "******" + m.substring(m.length() - 4); }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String firstNonBlank(String a, String b) { return !isBlank(a) ? a : b; }
    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
