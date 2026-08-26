package com.company.supportticketing.service;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.dto.request.*;
import com.company.supportticketing.dto.response.*;
import com.company.supportticketing.exception.DuplicateEmailException;
import com.company.supportticketing.repository.UserRepository;
import com.company.supportticketing.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw new DuplicateEmailException();
        var user = users.save(AppUser.builder().email(email).passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name().trim()).role(request.role()).build());
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getCreatedAt());
    }
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AppUser user = ((com.company.supportticketing.security.AppUserAdapter) auth.getPrincipal()).user();
        var token = jwtService.generate(user);
        return new AuthResponse(token.value(), token.expiresAt());
    }
}
