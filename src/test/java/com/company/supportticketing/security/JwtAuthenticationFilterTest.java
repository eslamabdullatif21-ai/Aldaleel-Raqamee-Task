package com.company.supportticketing.security;

import java.time.Instant;
import java.util.UUID;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.UserRole;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    private final JwtService jwtService = mock(JwtService.class);
    private final JwtUserCache userCache = mock(JwtUserCache.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userCache);

    @AfterEach void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test void verifiesTokenOnceAndAuthenticatesFromCachedPrincipal() throws Exception {
        UUID id = UUID.randomUUID();
        AppUser user = AppUser.builder().id(id).email("customer@example.com")
                .name("Customer").role(UserRole.CUSTOMER).build();
        var claims = new JwtService.JwtPrincipalClaims(id, user.getEmail(), user.getRole(), Instant.now().plusSeconds(60));
        when(jwtService.parseAndValidate("signed-token")).thenReturn(claims);
        when(userCache.get(user.getEmail())).thenReturn(user);
        when(jwtService.matches(claims, user)).thenReturn(true);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer signed-token");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertSame(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(jwtService, times(1)).parseAndValidate("signed-token");
        verify(userCache, times(1)).get(user.getEmail());
        verify(jwtService, times(1)).matches(claims, user);
    }

    @Test void invalidTokenContinuesWithoutDatabaseLookupOrAuthentication() throws Exception {
        when(jwtService.parseAndValidate("invalid-token")).thenThrow(new MalformedJwtException("invalid"));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userCache);
    }
}
