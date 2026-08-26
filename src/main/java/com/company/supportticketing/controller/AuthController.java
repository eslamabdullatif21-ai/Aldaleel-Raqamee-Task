package com.company.supportticketing.controller;

import com.company.supportticketing.dto.request.*;
import com.company.supportticketing.dto.response.*;
import com.company.supportticketing.service.AuthService;
import com.company.supportticketing.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService service;
    private final LoginRateLimiter loginRateLimiter;
    @PostMapping("/register") public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.check(httpRequest.getRemoteAddr());
        return service.login(request);
    }
}
