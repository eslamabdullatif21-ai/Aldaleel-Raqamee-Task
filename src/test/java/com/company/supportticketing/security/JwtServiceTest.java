package com.company.supportticketing.security;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "a-secure-test-secret-containing-more-than-32-bytes";
    private final AppUser user = AppUser.builder().email("customer@example.com").role(UserRole.CUSTOMER).build();
    @Test void generatedTokenRoundTrips() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        var token = service.generate(user);
        assertEquals(user.getEmail(), service.extractUsername(token.value()));
        assertTrue(service.isValid(token.value(), user));
    }
    @Test void tamperedTokenIsRejected() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        String token = service.generate(user).value();
        assertThrows(JwtException.class, () -> service.extractUsername(token.substring(0, token.length() - 2) + "xx"));
    }
    @Test void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, Duration.ofSeconds(-1));
        assertThrows(JwtException.class, () -> service.extractUsername(service.generate(user).value()));
    }
    @Test void weakSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("short", Duration.ofHours(1)));
    }
}
