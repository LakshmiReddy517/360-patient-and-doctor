package com.pcare.identity.service;

import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import com.pcare.identity.domain.User;
import com.pcare.identity.repo.UserRepository;
import com.pcare.identity.web.dto.AuthDtos.AuthResponse;
import com.pcare.identity.web.dto.AuthDtos.LoginRequest;
import com.pcare.identity.web.dto.AuthDtos.RefreshRequest;
import com.pcare.identity.web.dto.AuthDtos.RegisterPatientRequest;
import com.pcare.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/** Login, patient self-registration and token refresh. */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       UserService userService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User user = userRepository.findByUsernameIgnoreCase(req.username())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return buildResponse(user);
    }

    public AuthResponse registerPatient(RegisterPatientRequest req) {
        User user = userService.registerPatient(req);
        return buildResponse(user);
    }

    public AuthResponse registerPatientWithDocs(String username, String password, String fullName, String fatherName,
                                                String mobile, String email, String aadhaar, String pan,
                                                org.springframework.web.multipart.MultipartFile aadhaarImage,
                                                org.springframework.web.multipart.MultipartFile panImage) {
        User user = userService.registerPatientWithDocs(username, password, fullName, fatherName, mobile, email,
                aadhaar, pan, aadhaarImage, panImage);
        return buildResponse(user);
    }

    public AuthResponse refresh(RefreshRequest req) {
        Claims claims = jwtService.parse(req.refreshToken());
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BadRequestException("Provided token is not a refresh token");
        }
        User user = userRepository.findByUsernameIgnoreCase(claims.getSubject())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtService.getAccessTokenMinutes(),
                userService.toDto(user));
    }
}
