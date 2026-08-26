package com.company.supportticketing.controller;

import com.company.supportticketing.dto.request.LoginRequest;
import com.company.supportticketing.dto.response.AuthResponse;
import com.company.supportticketing.security.LoginRateLimiter;
import com.company.supportticketing.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock AuthService service;
    @Mock LoginRateLimiter limiter;
    @Mock HttpServletRequest httpRequest;

    @Test void loginChecksTheObservedClientAddressBeforeAuthentication() {
        LoginRequest request = new LoginRequest("customer@example.com", "Customer123!");
        AuthResponse expected = new AuthResponse("token", Instant.now().plusSeconds(3600));
        when(httpRequest.getRemoteAddr()).thenReturn("192.0.2.10");
        when(service.login(request)).thenReturn(expected);

        AuthResponse result = new AuthController(service, limiter).login(request, httpRequest);

        assertSame(expected, result);
        verify(limiter).check("192.0.2.10");
        verify(service).login(request);
    }
}
