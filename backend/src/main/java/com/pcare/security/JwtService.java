package com.pcare.security;

import com.pcare.identity.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/** Issues and validates signed JWT access/refresh tokens (HMAC-SHA256). */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessTokenMinutes(), ChronoUnit.MINUTES);
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("name", user.getFullName())
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public long getAccessTokenMinutes() {
        return properties.getAccessTokenMinutes();
    }
}
