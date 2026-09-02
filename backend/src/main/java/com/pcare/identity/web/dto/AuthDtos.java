package com.pcare.identity.web.dto;

import com.pcare.identity.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Data-transfer objects for authentication and account endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record RegisterPatientRequest(
            @NotBlank @Size(max = 120) String username,
            @NotBlank @Size(min = 6, max = 72) String password,
            @NotBlank @Size(max = 160) String fullName,
            @Size(max = 160) String fatherName,
            @Size(max = 30) String mobile,
            @Email @Size(max = 160) String email,
            @NotBlank @Pattern(regexp = "\\d{12}", message = "Aadhaar must be 12 digits") String aadhaar,
            @NotBlank @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]", message = "PAN must look like ABCDE1234F") String pan) {
    }

    public record CreateStaffRequest(
            @NotBlank @Size(max = 120) String username,
            @NotBlank @Size(min = 6, max = 72) String password,
            @NotBlank @Size(max = 160) String fullName,
            @Size(max = 30) String mobile,
            @Email @Size(max = 160) String email,
            Set<Role> roles) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresInMinutes,
            UserDto user) {
    }

    public record UserDto(
            Long id,
            String username,
            String fullName,
            String mobile,
            String email,
            boolean enabled,
            Set<Role> roles) {
    }
}
