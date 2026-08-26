package com.company.supportticketing.security;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "a-secure-test-secret-containing-more-than-32-bytes";
    private final AppUser user = AppUser.builder().id(UUID.randomUUID()).email("customer@example.com")
            .role(UserRole.CUSTOMER).build();
    @Test void generatedTokenRoundTrips() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        var token = service.generate(user);
        var claims = service.parseAndValidate(token.value());
        assertEquals(user.getId(), claims.userId());
        assertEquals(user.getEmail(), claims.email());
        assertEquals(user.getRole(), claims.role());
        assertTrue(service.matches(claims, user));
    }
    @Test void tamperedTokenIsRejected() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        String token = service.generate(user).value();
        assertThrows(JwtException.class, () -> service.parseAndValidate(token.substring(0, token.length() - 2) + "xx"));
    }
    @Test void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, Duration.ofSeconds(-1));
        assertThrows(JwtException.class, () -> service.parseAndValidate(service.generate(user).value()));
    }
    @Test void weakSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("short", Duration.ofHours(1)));
    }
    @Test void cannotIssueTokenWithoutUserId() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        AppUser transientUser = AppUser.builder().email("customer@example.com").role(UserRole.CUSTOMER).build();
        assertThrows(IllegalArgumentException.class, () -> service.generate(transientUser));
    }
    @Test void roleChangeDoesNotMatchExistingToken() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        var claims = service.parseAndValidate(service.generate(user).value());
        AppUser changed = AppUser.builder().id(user.getId()).email(user.getEmail()).role(UserRole.AGENT).build();
        assertFalse(service.matches(claims, changed));
    }
}
