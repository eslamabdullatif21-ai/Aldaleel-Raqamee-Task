package com.company.supportticketing.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.UserRole;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration:PT1H}") Duration expiration) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public AuthToken generate(AppUser user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("Cannot issue a token without a user ID");
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        String value = Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new AuthToken(value, expiresAt);
    }

    public JwtPrincipalClaims parseAndValidate(String token) {
        Claims claims = parse(token);
        String userId = claims.get("uid", String.class);
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        if (userId == null || email == null || role == null || claims.getExpiration() == null) {
            throw new MalformedJwtException("Token is missing required authentication claims");
        }
        try {
            return new JwtPrincipalClaims(
                    UUID.fromString(userId),
                    email,
                    UserRole.valueOf(role),
                    claims.getExpiration().toInstant());
        } catch (IllegalArgumentException exception) {
            throw new MalformedJwtException("Token contains invalid authentication claims", exception);
        }
    }

    public boolean matches(JwtPrincipalClaims claims, AppUser user) {
        return claims.userId().equals(user.getId())
                && claims.email().equalsIgnoreCase(user.getEmail())
                && claims.role() == user.getRole();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record AuthToken(String value, Instant expiresAt) {}

    public record JwtPrincipalClaims(
            UUID userId,
            String email,
            UserRole role,
            Instant expiresAt) {}
}
