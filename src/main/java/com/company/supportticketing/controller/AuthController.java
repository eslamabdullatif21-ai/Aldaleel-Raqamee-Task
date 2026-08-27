package com.company.supportticketing.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.company.supportticketing.dto.request.LoginRequest;
import com.company.supportticketing.dto.request.RegisterRequest;
import com.company.supportticketing.dto.response.AuthResponse;
import com.company.supportticketing.dto.response.UserResponse;
import com.company.supportticketing.security.LoginRateLimiter;
import com.company.supportticketing.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        loginRateLimiter.check(httpRequest.getRemoteAddr());
        return service.login(request);
    }
}
