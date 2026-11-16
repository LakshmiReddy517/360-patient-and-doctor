package com.pcare.notification.otp;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public OTP endpoints for phone verification / mobile login. */
@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @Operation(summary = "Request an OTP to a mobile number (SMS via MSG91)")
    @PostMapping("/request")
    public Map<String, Object> request(@RequestBody OtpRequest req) {
        otpService.requestOtp(req.mobile(), req.purpose());
        return Map.of("sent", true, "message", "If the number is valid, an OTP has been sent.");
    }

    @Operation(summary = "Verify an OTP code")
    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody OtpVerify req) {
        boolean ok = otpService.verifyOtp(req.mobile(), req.purpose(), req.code());
        return Map.of("verified", ok);
    }

    public record OtpRequest(@NotBlank String mobile, String purpose) {}
    public record OtpVerify(@NotBlank String mobile, String purpose, @NotBlank String code) {}
}
