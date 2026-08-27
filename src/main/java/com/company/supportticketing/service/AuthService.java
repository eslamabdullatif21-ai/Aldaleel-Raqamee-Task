package com.company.supportticketing.service;

import java.util.Locale;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.dto.request.LoginRequest;
import com.company.supportticketing.dto.request.RegisterRequest;
import com.company.supportticketing.dto.response.AuthResponse;
import com.company.supportticketing.dto.response.UserResponse;
import com.company.supportticketing.exception.DuplicateEmailException;
import com.company.supportticketing.exception.PermissionDeniedException;
import com.company.supportticketing.repository.UserRepository;
import com.company.supportticketing.security.AppUserAdapter;
import com.company.supportticketing.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.role() != UserRole.CUSTOMER) {
            throw new PermissionDeniedException("Public registration is limited to customer accounts");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        var user = users.save(AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .role(UserRole.CUSTOMER)
                .build());
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AppUser user = ((AppUserAdapter) auth.getPrincipal()).user();
        var token = jwtService.generate(user);
        return new AuthResponse(token.value(), token.expiresAt());
    }
}
