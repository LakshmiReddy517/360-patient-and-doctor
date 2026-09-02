package com.pcare.identity.web;

import com.pcare.identity.domain.User;
import com.pcare.identity.repo.UserRepository;
import com.pcare.identity.service.AuthService;
import com.pcare.identity.service.UserService;
import com.pcare.identity.web.dto.AuthDtos.AuthResponse;
import com.pcare.identity.web.dto.AuthDtos.CreateStaffRequest;
import com.pcare.identity.web.dto.AuthDtos.LoginRequest;
import com.pcare.identity.web.dto.AuthDtos.RefreshRequest;
import com.pcare.identity.web.dto.AuthDtos.RegisterPatientRequest;
import com.pcare.identity.web.dto.AuthDtos.UserDto;
import com.pcare.common.exception.NotFoundException;
import com.pcare.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Login, patient self-registration, token refresh and staff provisioning")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserService userService, UserRepository userRepository) {
        this.authService = authService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Login with username (email/mobile) and password")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Patient/guardian self-registration (JSON)")
    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse register(@Valid @RequestBody RegisterPatientRequest req) {
        return authService.registerPatient(req);
    }

    @Operation(summary = "Patient self-registration with government ID (Aadhaar/PAN) images (multipart)")
    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuthResponse registerWithDocs(
            @org.springframework.web.bind.annotation.RequestParam String username,
            @org.springframework.web.bind.annotation.RequestParam String password,
            @org.springframework.web.bind.annotation.RequestParam String fullName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String fatherName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String mobile,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam String aadhaar,
            @org.springframework.web.bind.annotation.RequestParam String pan,
            @org.springframework.web.bind.annotation.RequestPart(required = false) org.springframework.web.multipart.MultipartFile aadhaarImage,
            @org.springframework.web.bind.annotation.RequestPart(required = false) org.springframework.web.multipart.MultipartFile panImage) {
        return authService.registerPatientWithDocs(username, password, fullName, fatherName, mobile, email,
                aadhaar, pan, aadhaarImage, panImage);
    }

    @Operation(summary = "Exchange a refresh token for a new access token")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }

    @Operation(summary = "Provision a staff account (admin only)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/staff")
    public UserDto createStaff(@Valid @RequestBody CreateStaffRequest req) {
        return userService.toDto(userService.createStaff(req));
    }

    @Operation(summary = "Current authenticated user profile")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        Long uid = SecurityUtils.currentUserId().orElseThrow(() -> new NotFoundException("Not authenticated"));
        User user = userRepository.findById(uid).orElseThrow(() -> new NotFoundException("User not found"));
        return ResponseEntity.ok(userService.toDto(user));
    }
}
